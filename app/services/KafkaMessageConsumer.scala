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

  def receiveMessages(): Future[Unit] = Future {
    logger.info("Starting to receive messages from Kafka")
    try {
      while (true) {
        val records = consumer.poll(java.time.Duration.ofMillis(100))
        for (record <- records.asScala) {
          logger.info(s"Consumed record: key=${record.key()}, value=${record.value()}")
          try {
            val Array(senderName, content) = record.value().split(": ", 2)
            dbService.saveMessage(record.key(), senderName, content, System.currentTimeMillis()).onComplete {
              case scala.util.Success(_) =>
                logger.info(s"Message saved to database: senderName=$senderName, receiverName=${record.key()}, content=$content")
              case scala.util.Failure(exception) =>
                logger.error(s"Failed to save message: ${exception.getMessage}", exception)
            }
          } catch {
            case e: Exception =>
              logger.error(s"Error processing record: ${record.value()}, error: ${e.getMessage}", e)
          }
        }
      }
    } catch {
      case e: Exception =>
        logger.error(s"Error while consuming messages: ${e.getMessage}", e)
    } finally {
      consumer.close()
      logger.info("Kafka consumer closed")
    }
  }
}