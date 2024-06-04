package controllers

import javax.inject._
import play.api.libs.json._
import play.api.mvc._
import services.{DatabaseService, KafkaMessageConsumer, KafkaMessageProducer}

import scala.concurrent.{ExecutionContext, Future}
import models.Message
import models.Message.messageFormat
import play.api.inject.ApplicationLifecycle


@Singleton
class ChatController @Inject()(
                                cc: ControllerComponents,
                                kafkaProducer: KafkaMessageProducer,
                                dbService: DatabaseService,
                                kafkaConsumer: KafkaMessageConsumer,
                                lifeCycle: ApplicationLifecycle
                              )(implicit ec: ExecutionContext) extends AbstractController(cc) {
  println("ChatController initialized")

  /**
   * Renders the chat page for the given user.
   * If the username is provided as a query parameter, renders the chat page for that user.
   * Otherwise, returns an Unauthorized or redirects to the login page.
   */
  def chatPage = Action { implicit request =>
    request.getQueryString("username").map { username =>
      Ok(views.html.chat(username))
    }.getOrElse {
      Unauthorized("You are not logged in")
      Redirect("http://localhost:9299/login")
    }
  }

  /**
   * Sends a message to the Kafka topic.
   * Expects JSON data containing message details in the request body.
   */
  def sendMessage = Action.async(parse.json) { implicit request =>
    request.body.validate[Message].fold(
      errors => {
        Future.successful(BadRequest(Json.obj("status" -> "error", "message" -> JsError.toJson(errors))))
      },
      sendMessageRequest => {
        kafkaProducer.sendMessage(sendMessageRequest.senderName, sendMessageRequest.receiverName, sendMessageRequest.content, sendMessageRequest.timestamp).map { _ =>
          Ok(Json.obj("status" -> "Message sent"))
        }
      }
    )
  }
  /**
   * Fetches messages for the given user from the database.
   * Expects the username as a path parameter.
   */

  def fetchMessages(userName: String) = Action.async { implicit request =>
    if (userName.trim.isEmpty) {
      Future.successful(BadRequest("User ID is missing"))
    } else {
      dbService.getMessagesForUser(userName).map { messages =>
        val messageList = messages.map { message =>
          s"{ senderName: '${message.senderName}', content: '${message.content}' }"
        }.mkString(",")
        Ok(messageList)
      }
    }
  }
  /**
   * Renders the index page.
   */
  def index() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index())
  }
}