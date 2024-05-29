package controllers

import akka.actor.{ActorRef, ActorSystem}

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
                                dbService: DatabaseService,// Inject KafkaMessageConsumer actor
                                kafkaConsumer: KafkaMessageConsumer,
                                lifeCycle: ApplicationLifecycle
                              )(implicit ec: ExecutionContext) extends AbstractController(cc) {

  kafkaConsumer.receiveMessages()

  def sendMessage = Action.async(parse.json) { implicit request =>
    request.body.validate[Message].fold(
      errors => {
        Future.successful(BadRequest(Json.obj("status" -> "error", "message" -> JsError.toJson(errors))))
      },
      sendMessageRequest => {
        kafkaProducer.sendMessage(sendMessageRequest.senderId, sendMessageRequest.receiverId, sendMessageRequest.content, sendMessageRequest.timestamp).map{_ =>
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
        kafkaProducer.sendMessage(sendMessageRequest.senderId, sendMessageRequest.receiverId, sendMessageRequest.content, sendMessageRequest.timestamp).map{_ =>
          Ok(Json.obj("status" -> "Message sent"))
        }
      }
    )
  }

  def fetchMessages(userId: String) = Action.async { implicit request =>
    dbService.getMessagesForUser(userId).map { messages =>
      val messageContents = messages.map { message =>
        Json.obj("content" -> message.content)
      }
      Ok(Json.toJson(messageContents))
    }
  }

  def index() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index())
  }

  def sender() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.sender())
  }

  def receiver() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.receiver())
  }
}