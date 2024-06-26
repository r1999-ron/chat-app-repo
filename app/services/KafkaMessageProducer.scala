package services

import java.util.Properties
import javax.inject.Singleton
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig, ProducerRecord}
import org.apache.kafka.common.serialization.StringSerializer

import scala.concurrent.{ExecutionContext, Future}
import javax.inject.Inject

/**
 * A singleton class responsible for producing messages to Kafka topics asynchronously.
 *
 * @param config The Play configuration containing Kafka-related settings.
 * @param ec     The execution context to perform asynchronous operations.
 */

@Singleton
class KafkaMessageProducer @Inject()(config: play.api.Configuration)(implicit ec : ExecutionContext){

  private val KafkaTopic: String = config.get[String]("kafka.topic")
  private val NotificationTopic: String = config.get[String]("kafka.notification.topic")

  private val kafkaProducerProps: Properties = {
    val props = new Properties()
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.get[String]("kafka.bootstrap.servers"))
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer].getName)
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer].getName)
    props

  }

  // Create Kafka producer
  private val producer = new KafkaProducer[String, String](kafkaProducerProps)

  // Method to send message asynchronously
  /**
   * Sends a message to the Kafka topic asynchronously.
   *
   * @param senderName   The name of the message sender.
   * @param receiverName The name of the message receiver.
   * @param content      The content of the message.
   * @param timestamp    The timestamp of the message.
   * @return A future indicating completion of the operation.
   */
  def sendMessage(senderName: String, receiverName: String, content: String, timestamp: Long): Future[Unit] = Future {
    val message = s"$senderName: $content"
    println(s"Sending message to Kafka: $message")
    val record = new ProducerRecord[String, String](KafkaTopic, receiverName, s"$senderName: $content")
    println(s"Sending message: $senderName to $receiverName - $content and timestamp is $timestamp")
    println(s"Record: $record")
    /*val notificationRecord = new ProducerRecord[String, String](NotificationTopic, receiverName, s"New message from $senderName: $content")
    producer.send(notificationRecord)*/
    producer.send(record)
  }
  // Add shutdown hook to close the producer when application exits
  sys.addShutdownHook {
    producer.close()
  }
}
