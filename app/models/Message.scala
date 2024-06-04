package models
import play.api.libs.json._

case class Message(senderName: String, receiverName: String, content: String, timestamp: Long)

object Message {
  implicit val messageFormat: Format[Message] = Json.format[Message]
}