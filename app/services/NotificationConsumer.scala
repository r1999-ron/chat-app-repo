package services
import java.util.Properties
import javax.inject.{Inject, Singleton}
import org.apache.kafka.clients.consumer.{ConsumerConfig, KafkaConsumer}
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._

@Singleton
class NotificationConsumer @Inject()(config: play.api.Configuration, lifecycle: play.api.inject.ApplicationLifecycle)(implicit ec: ExecutionContext) {

  private val kafkaConsumerProps: Properties = {
    val props = new Properties()
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, config.get[String]("kafka.bootstrap.servers"))
    props.put(ConsumerConfig.GROUP_ID_CONFIG, config.get[String]("kafka.group.id"))
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
    props
  }

  private val consumer = new KafkaConsumer[String, String](kafkaConsumerProps)
  consumer.subscribe(List(config.get[String]("kafka.notification.topic")).asJava)

  def receiveNotifications(): Future[Unit] = Future {
    println("Starting to receive notifications from Kafka")
    try {
      while (true) {
        val records = consumer.poll(java.time.Duration.ofMillis(100))
        for (record <- records.asScala) {
          println(s"Received notification: ${record.value()}")
          // Logic to send notifications to users
        }
      }
    } catch {
      case e: Exception =>
        println(s"Error while consuming notifications: ${e.getMessage}", e)
    } finally {
      consumer.close()
      println("Notification consumer closed")
    }
  }

  // Start consuming notifications when the application starts
  lifecycle.addStopHook { () =>
    println("Stopping Notification Consumer")
    Future.successful(consumer.close())
  }

  // Start consuming notifications
  receiveNotifications()
}