package services

import javax.inject.Inject
import models.Message
import org.slf4j.LoggerFactory
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

/**
 * Service responsible for interacting with the database for message-related operations.
 */

class DatabaseService @Inject()(dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) {
 // private val logger = LoggerFactory.getLogger(this.getClass)
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

  // Query object for the "chat_messages" table
  private val messages = TableQuery[MessagesTable]

  /**
   * Saves a message to the database.
   *
   * @param receiverName The username of the message receiver.
   * @param senderName The username of the message sender.
   * @param content The content of the message.
   * @param timestamp The timestamp of the message.
   * @return A Future indicating the success or failure of the operation.
   */
  def saveMessage(receiverName: String, senderName: String, content: String, timestamp: Long): Future[Unit] = {
    println(s"Saving message: senderName=$senderName, receiverName=$receiverName, content=$content, timestamp=$timestamp")
    db.run {
      messages += Message(senderName, receiverName, content, timestamp)
    }.map(_ => {
      println("Message saved successfully")
    }).recover {
      case ex: Exception =>
        println(s"Error saving message: ${ex.getMessage}")
        throw new RuntimeException("Database operation failed!!!")
    }
  }
  /**
   * Retrieves messages for a user from the database.
   *
   * @param userName The username of the user to fetch messages for.
   * @return A Future containing the list of messages for the user.
   */
  def getMessagesForUser(userName: String): Future[List[Message]] = {
    db.run {
      messages.filter(_.receiverName =!= userName).result
    }.map { messages =>
      messages.toList
    }
  }
}