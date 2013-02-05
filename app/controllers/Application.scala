package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.functional.syntax._
import play.api.libs.iteratee._
import play.api.libs.json._
import play.api.libs.EventSource
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util._

import models._

object Application extends Controller {

  implicit def author = User("bob")

  def index = Action { implicit request =>
    Async {
      Message.all.map { msgs =>
        Ok(views.html.index(msgs))
      }
    }
  }

  def listen = Action { implicit request =>
    implicit val format = MessageJsonFormat
    Ok.feed(Message.enumerate &> Json.toJson ><> EventSource()).as("text/event-stream")
  }

  val msgForm = Form(single(
    "text" -> nonEmptyText
  ))

  def sendMessage = Action { implicit request =>
    msgForm.bindFromRequest.fold(
      err => {
        Redirect(routes.Application.index)
      },
      text => {
        Async {
          Message.create(text).map {
            case Success(_) =>
              Redirect(routes.Application.index)
            case Failure(e) =>
              Logger.error("Error during creation", e)
              InternalServerError("Internal error")
          }
        }
      }
    )
  }

  def sendMessageAsync = Action { implicit request =>
    msgForm.bindFromRequest.fold(
      err => {
        BadRequest(Json.obj("error" -> "Bad request"))
      },
      text => {
        Async {
          Message.create(text).map {
            case Success(_) =>
              Created(Json.obj())
            case Failure(e) =>
              Logger.error("Error during creation", e)
              InternalServerError(Json.obj("error" -> "Internal error"))
          }
        }
      }
    )
  }

}