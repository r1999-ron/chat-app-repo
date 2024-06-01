package services

import akka.actor.Actor
import akka.actor.Props

import java.util.Properties
import javax.inject.Singleton
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig, ProducerRecord}
import org.apache.kafka.common.serialization.StringSerializer

import scala.concurrent.{ExecutionContext, Future}
import javax.inject.{Inject}

@Singleton
class KafkaMessageProducer @Inject()(config: play.api.Configuration)(implicit ec : ExecutionContext){

  private val KafkaTopic: String = config.get[String]("kafka.topic")

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
  def sendMessage(senderName: String, receiverName: String, content: String, timestamp: Long): Future[Unit] = Future {
    val message = s"$senderName: $content"
    println(s"Sending message to Kafka: $message")
    val record = new ProducerRecord[String, String](KafkaTopic, receiverName, s"$senderName: $content")
    println(s"Sending message: $senderName to $receiverName - $content and timestamp is $timestamp")
    println(s"Record: $record")
    producer.send(record)
  }
  // Add shutdown hook to close the producer when application exits
  sys.addShutdownHook {
    producer.close()
  }
}
