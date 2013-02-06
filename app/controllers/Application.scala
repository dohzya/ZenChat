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
import scala.concurrent._
import scala.concurrent.duration._
import scala.util._

import models._

object Application extends Controller with Authentication {

  def index = Authenticated { implicit user => implicit request =>
    Async {
      Message.all.map { msgs =>
        Ok(views.html.index(msgs))
      }
    }
  }

  def chat = WebSocket.async[JsValue] { implicit request =>
    authenticated[Future[(Iteratee[JsValue,_], Enumerator[JsValue])]] { user =>
      Logger.debug(s"chat ($user)")
      models.ChatRoom.join(user)
    }.flatMap(_.getOrElse { throw new java.lang.RuntimeException("Not authenticated") })
  }

}
