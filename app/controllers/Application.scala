package controllers

import akka.pattern.ask
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
      ChatServer.listRooms(user).map { rooms =>
        Ok(views.html.index(rooms))
      }
    }
  }

  def room(roomName: String) = Authenticated { implicit user => implicit request =>
    Async {
      Message.all(roomName).map { msgs =>
        Ok(views.html.room(roomName, msgs))
      }
    }
  }


  def chat(roomName: String) = WebSocket.async[JsValue] { implicit request =>
    authenticated[Future[(Iteratee[JsValue,_], Enumerator[JsValue])]] { user =>
      Logger("chat").debug(s"User $user connecting to chatroom $roomName")
      ChatServer.join(roomName, user)
    }.flatMap(_.getOrElse { throw new java.lang.RuntimeException("Not authenticated") })
  }

}
