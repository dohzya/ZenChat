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

  var i = 0  // ugly for works for quick tests :-)
  def chat = WebSocket.async[JsValue] { request  =>
    i = i+1
    val username = s"toto$i"
    Logger.debug(s"chat ($username)")
    models.ChatRoom.join(username)
  }

}