package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
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

}