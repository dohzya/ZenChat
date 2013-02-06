package models

import akka.actor.{Actor, ActorSystem, Props}
import akka.actor.{Actor, Props}
import akka.pattern.ask
import akka.util.Timeout
import play.api.Play.current
import play.api._
import play.api.libs.concurrent._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.iteratee._
import play.api.libs.json._
import scala.concurrent._
import scala.concurrent.duration._

import models._

object ChatRoom {

  implicit val timeout = Timeout(1 second)

  lazy val default = {
    val roomActor = Akka.system.actorOf(Props[ChatRoom])
    roomActor
  }

  def join(user: User): Future[(Iteratee[JsValue,_], Enumerator[JsValue])] = {
    (default ? Join(user)).map {

      case Connected(enumerator) =>
        Logger.debug("$username is connected")
        // Create an Iteratee to consume the feed
        val iteratee = Iteratee.foreach[JsValue] { event =>
          // Put clever stuff here (commands, etc)
          default ! Talk(user, (event \ "message").as[String])
        }.mapDone { _ =>
          default ! Quit(user)
        }
        (iteratee, enumerator)

      case CannotConnect(error) =>
        Logger.debug("$username is not connected ($error)")
        // Connection error
        // A finished Iteratee sending EOF
        val iteratee = Done[JsValue, Unit]((), Input.EOF)
        // Send an error and close the socket
        val msg = Json.obj("error" -> error)
        val enumerator = Enumerator[JsValue](msg) andThen Enumerator.enumInput(Input.EOF)
        (iteratee, enumerator)
    }
  }

}

class ChatRoom extends Actor {

  var members = Set.empty[User]
  val (chatEnumerator, chatChannel) = Concurrent.broadcast[JsValue]

  def receive = {

    case Join(user) => {
      if(members.contains(user)) {
        sender ! CannotConnect("You are already in this room")
      } else {
        members = members + user
        sender ! Connected(chatEnumerator)
        self ! NotifyJoin(user)
      }
    }

    case NotifyJoin(user) => {
      notifyAll("info", "has entered the room")(user)
    }

    case Talk(user, text) => {
      notifyAll("message", text)(user)
    }

    case Quit(user) => {
      members = members - user
      notifyAll("info", "has left the room")(user)
    }

  }

  def notifyAll(kind: String, text: String)(implicit user: User) {
    implicit val format = MessageJsonFormat
    // create the message…
    val msg = Message(text)
    // then save it in DB…
    Message.insert(msg)
    // and broadcast it
    chatChannel.push(Json.toJson(msg))
  }

}

case class Join(user: User)
case class Quit(user: User)
case class Talk(user: User, text: String)
case class NotifyJoin(user: User)

case class Connected(enumerator:Enumerator[JsValue])
case class CannotConnect(msg: String)
