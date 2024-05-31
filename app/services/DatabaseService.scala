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

  private class MessagesTable(tag: Tag) extends Table[Message](tag, "messages") {
    def senderId = column[String]("sender_id")
    def receiverId = column[String]("receiver_id")
    def content = column[String]("content")
    def timestamp = column[Long]("timestamp")
    def * = (senderId, receiverId, content, timestamp) <> ((Message.apply _).tupled, Message.unapply)
  }

  private val messages = TableQuery[MessagesTable]

  def saveMessage(receiverId: String, senderId: String, content: String, timestamp: Long): Future[Unit] = {
    println(s"Saving message: senderId=$senderId, receiverId=$receiverId, content=$content, timestamp=$timestamp")
    db.run {
      messages += Message(senderId, receiverId, content, timestamp)
    }.map(_ => {
      println("Message saved successfully")
    }).recover {
      case ex: Exception =>
        println(s"Error saving message: ${ex.getMessage}")
    }
  }
  def getMessagesForUser(userId: String): Future[List[Message]] = db.run {
    messages.filter(_.receiverId != userId).result
  }.map(_.toList)
}