package models

import akka.actor._
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

object ChatServer {

  implicit val timeout = Timeout(5 second)

  lazy val default: Room = Akka.system.actorOf(Props[ChatServer])

  def join(roomName: String, user: User): Future[(Iteratee[JsValue,_], Enumerator[JsValue])] = {
    (default ? Join(roomName, user)).map {

      case Connected(room, enumerator) =>
        Logger("chat.server").debug("$username is connected")
        // Create an Iteratee to consume the feed
        val iteratee = Iteratee.foreach[JsValue] { event =>
          // Put clever stuff here (commands, etc)
          Logger("chat.server").debug(s"Receive event: $event")
          room ! Talk(user, (event \ "message").as[String])
        }.mapDone { _ =>
          room ! Quit(user)
        }
        (iteratee, enumerator)

      case CannotConnect(error) =>
        Logger("chat.server").debug("$username is not connected ($error)")
        // Connection error
        // A finished Iteratee sending EOF
        val iteratee = Done[JsValue, Unit]((), Input.EOF)
        // Send an error and close the socket
        val msg = Json.obj("error" -> error)
        val enumerator = Enumerator[JsValue](msg) andThen Enumerator.enumInput(Input.EOF)
        (iteratee, enumerator)
    }
  }

  def listRooms(user: User): Future[Seq[String]] = {
    (default ? ListRooms(user)).map {
      case RoomList(list) => list
    }
  }

}

class ChatServer extends Actor {

  implicit val timeout = Timeout(5 second)

  var rooms = Map.empty[String, Room]

  def createRoom(name: String): Room = {
    Akka.system.actorOf(Props(new ChatRoom(name)))
  }

  def receive = {
    case Join(roomName, user) =>
    Logger("chat.server").debug(s"User $user connecting to room $roomName")
      val room = rooms.get(roomName).getOrElse {
        val room = createRoom(roomName)
        rooms = rooms + (roomName -> room)
        room
      }
      room ! Join2(user, sender)
    case ListRooms(user) =>
      sender ! RoomList(rooms.keys.toSeq)
  }

}

class ChatRoom(roomName: String) extends Actor {

  implicit val timeout = Timeout(5 second)

  var members = Set.empty[User]
  val (chatEnumerator, chatChannel) = Concurrent.broadcast[JsValue]

  def receive = {

    case Join2(user, target) => {
      if(members.contains(user)) {
        target ! CannotConnect("You are already in this room")
      } else {
        members = members + user
        target ! Connected(self, chatEnumerator)
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
    Logger("chat.room."+roomName).debug(s"notifyAll($kind, $text)($user)")
    implicit val format = MessageJsonFormat
    // create the message…
    val msg = Message(roomName, text)
    // then save it in DB…
    Message.insert(msg)
    // and broadcast it
    chatChannel.push(Json.toJson(msg))
  }

}

case class Join(room: String, user: User)  // When talking to server
case class Join2(user: User, target: ActorRef)  // When server talk to room
case class Quit(user: User)
case class Talk(user: User, text: String)
case class NotifyJoin(user: User)
case class RoomList(rooms: Seq[String])

case class ListRooms(user: User)
case class Connected(roomName: Room, enumerator:Enumerator[JsValue])
case class CannotConnect(msg: String)
