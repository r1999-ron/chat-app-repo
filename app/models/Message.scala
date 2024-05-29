package models
import play.api.libs.json._

case class Message(senderId: String, receiverId: String, content: String, timestamp: Long)

object Message {
  implicit val messageFormat: Format[Message] = Json.format[Message]
}