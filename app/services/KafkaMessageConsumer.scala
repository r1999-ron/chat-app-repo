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

@Singleton
class KafkaMessageConsumer @Inject()(config: Configuration, dbService: DatabaseService, lifecycle: ApplicationLifecycle)(implicit ec: ExecutionContext) {

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

  def receiveMessages(): Future[Unit] = Future {
    println("Entering receiveMessage() method")
    try {
      while (true) {
        val records = consumer.poll(java.time.Duration.ofMillis(100))
        for (record <- records.asScala) {
          println(s"Consumed record: key=${record.key()}, value=${record.value()}")
          try {
            val Array(senderId, content) = record.value().split(": ", 2)
           // val timestamp = timestampStr.toLong
              dbService.saveMessage(record.key(), senderId, content, System.currentTimeMillis())
            println(s"Message saved to database: senderId=$senderId, receiverId=${record.key()}, content=$content")
          } catch {
            case e: Exception =>
              println(s"Error processing record: ${record.value()}, error: ${e.getMessage}")
          }
        }
      }
    } catch {
      case e: Exception =>
        println(s"Error while consuming messages: ${e.getMessage}")
    } finally {
      consumer.close() // Close the Kafka consumer when done
    }
  }
}
