package controllers

import akka.actor.{ActorRef, ActorSystem}

import javax.inject._
import play.api.libs.json._
import play.api.mvc._
import services.{DatabaseService, KafkaMessageConsumer, KafkaMessageProducer}
import play.api.Configuration

import scala.concurrent.{ExecutionContext, Future}
import models.Message
import models.Message.messageFormat
import play.api.inject.ApplicationLifecycle


@Singleton
class ChatController @Inject()(
                                cc: ControllerComponents,
                                kafkaProducer: KafkaMessageProducer,
                                dbService: DatabaseService,// Inject KafkaMessageConsumer actor
                                kafkaConsumer: KafkaMessageConsumer,
                                lifeCycle: ApplicationLifecycle,
                                config: Configuration
                              )(implicit ec: ExecutionContext) extends AbstractController(cc) {

  kafkaConsumer.receiveMessages()

  private val loginUrl: String = config.get[String]("login.url")
  private val chatappUrl : String = config.get[String]("chatapp.url")

  /*def chatPage = Action { implicit request =>
    println("Inside chatPage method")
    println("Session data:")
    println(request.session.data)
    request.session.get("username").map { username =>
      println(s"Username found in session: $username")
      Ok(views.html.chat(username))
    }.getOrElse {
      println("Username not found in session")
      Unauthorized("You are not logged in")
    }
  }*/
  /*def chatPage = Action { implicit request =>
    val currentTime = System.currentTimeMillis()
    val sessionExpiry = request.session.get("expiry").flatMap(_.toLongOption)

    println(s"Current time: $currentTime")
    println(s"Session expiry time: $sessionExpiry")

    println("Session data: " + request.session.data)

    if (sessionExpiry.exists(_ < currentTime)) {
      println("Session expired. Redirecting to login page.")
      Redirect("http://localhost:9299/login").withNewSession
    } else {
      request.session.get("username").map { username =>
        println(s"User $username is logged in.")
        Ok(views.html.chat(username))
      }.getOrElse {
        println("User is not logged in.")
        Unauthorized("You are not logged in")
      }
    }
  }*/

  def chatPage = Action { implicit request =>
    request.getQueryString("username").map { username =>
      Ok(views.html.chat(username, loginUrl))
    }.getOrElse {
      Redirect(loginUrl)
    }
  }


  def sendMessage = Action.async(parse.json) { implicit request =>
    request.body.validate[Message].fold(
      errors => {
        Future.successful(BadRequest(Json.obj("status" -> "error", "message" -> JsError.toJson(errors))))
      },
      sendMessageRequest => {
        kafkaProducer.sendMessage(sendMessageRequest.senderId, sendMessageRequest.receiverId, sendMessageRequest.content, sendMessageRequest.timestamp).map { _ =>
          Ok(Json.obj("status" -> "Message sent"))
        }
      }
    )
  }

  def sendMessageToUser = Action.async(parse.json) { implicit request =>
    request.body.validate[Message].fold(
      errors => {
        Future.successful(BadRequest(Json.obj("status" -> "error", "message" -> JsError.toJson(errors))))
      },
      sendMessageRequest => {
        kafkaProducer.sendMessage(sendMessageRequest.senderId, sendMessageRequest.receiverId, sendMessageRequest.content, sendMessageRequest.timestamp).map { _ =>
          Ok(Json.obj("status" -> "Message sent"))
        }
      }
    )
  }

  def fetchMessages(userId: String) = Action.async { implicit request =>
    if (userId.trim.isEmpty) {
      Future.successful(BadRequest("User ID is missing"))
    } else {
      dbService.getMessagesForUser(userId).map { messages =>
        Ok(Json.toJson(messages))
      }
    }
  }
  def index() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index(chatappUrl))
  }
}