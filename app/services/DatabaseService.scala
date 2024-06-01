package services

import javax.inject.Inject
import models.Message
import org.slf4j.LoggerFactory
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

class DatabaseService @Inject()(dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val dbConfig = dbConfigProvider.get[JdbcProfile]
  import dbConfig._
  import profile.api._

  private class MessagesTable(tag: Tag) extends Table[Message](tag, "chat_messages") {
    def senderName = column[String]("sender_name")
    def receiverName = column[String]("receiver_name")
    def content = column[String]("content")
    def timestamp = column[Long]("timestamp")
    def * = (senderName, receiverName, content, timestamp) <> ((Message.apply _).tupled, Message.unapply)
  }

  private val messages = TableQuery[MessagesTable]

  def saveMessage(receiverName: String, senderName: String, content: String, timestamp: Long): Future[Unit] = {
    println(s"Saving message: senderId=$senderName, receiverId=$receiverName, content=$content, timestamp=$timestamp")
    db.run {
      messages += Message(senderName, receiverName, content, timestamp)
    }.map(_ => {
      println("Message saved successfully")
    }).recover {
      case ex: Exception =>
        println(s"Error saving message: ${ex.getMessage}")
    }
  }
  def getMessagesForUser(userName: String): Future[List[Message]] = {
    db.run {
      messages.filter(_.receiverName != userName).result
    }.map { messages =>
      messages.toList
    }
  }
}