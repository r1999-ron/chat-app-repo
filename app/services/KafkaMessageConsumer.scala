package services

import com.fasterxml.jackson.databind.deser.std.StringDeserializer

import java.util.Properties
import javax.inject.{Inject, Singleton}
import org.apache.kafka.clients.consumer.{ConsumerConfig, KafkaConsumer}
import org.apache.kafka.common.serialization.StringSerializer

import scala.concurrent.{ExecutionContext, Future}
import play.api.Configuration
import play.api.inject.ApplicationLifecycle

import scala.jdk.CollectionConverters._
import org.slf4j.LoggerFactory

import java.util.concurrent.ForkJoinPool

@Singleton
class KafkaMessageConsumer @Inject()(config: Configuration, dbService: DatabaseService, lifecycle: ApplicationLifecycle)(implicit ec: ExecutionContext) {

  private val logger = LoggerFactory.getLogger(this.getClass)

  private val kafkaConsumerProps: Properties = {
    val props = new Properties()
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, config.get[String]("kafka.bootstrap.servers"))
    props.put(ConsumerConfig.GROUP_ID_CONFIG, config.get[String]("kafka.group.id"))
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
    props
  }

  private val consumer = new KafkaConsumer[String, String](kafkaConsumerProps)
  consumer.subscribe(List(config.get[String]("kafka.topic")).asJava)

  //Creating a dedicated execution context for the consumer
  private implicit val consumerEc : ExecutionContext = ExecutionContext.fromExecutor(new ForkJoinPool(1))

  /**
   * Starts the Kafka consumer to continuously poll messages from Kafka.
   * Messages are processed asynchronously and saved to the database.
   */
  def startConsumer():Unit =  {
    Future {
      println("Starting to receive messages from Kafka")
      try {
        while (true) {
          val records = consumer.poll(java.time.Duration.ofMillis(100))
          for (record <- records.asScala) {
            println(s"Consumed record: key=${record.key()}, value=${record.value()}")
            try {
              val Array(senderName, content) = record.value().split(": ", 2)
              dbService.saveMessage(record.key(), senderName, content, System.currentTimeMillis()).onComplete {
                case scala.util.Success(_) =>
                  println(s"Message saved to database: senderName=$senderName, receiverName=${record.key()}, content=$content")
                case scala.util.Failure(exception) =>
                  println(s"Failed to save message: ${exception.getMessage}", exception)
              }(ec)
            } catch {
              case e: Exception =>
                println(s"Error processing record: ${record.value()}, error: ${e.getMessage}", e)
            }
          }
        }
      } catch {
        case e: Exception =>
          println(s"Error while consuming messages: ${e.getMessage}", e)
      } finally {
        consumer.close()
        println("Kafka consumer closed")
      }
    }(consumerEc)
  }
  // Adding shutdown hook to close the consumer when application exits
  lifecycle.addStopHook{() =>
    Future.successful(consumer.close())
  }
  // Starting the Kafka consumer upon initialization
  startConsumer()

}