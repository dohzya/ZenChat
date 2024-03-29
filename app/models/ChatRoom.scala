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

  def join(roomName: String)(implicit user: User): Future[(Iteratee[JsValue,_], Enumerator[JsValue])] = {
    (default ? Join(roomName, user)).map {

      case Connected(room, enumerator) =>
        Logger("chat.server").debug(s"$user is connected")
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

  def listRooms()(implicit user: User): Future[Seq[String]] = {
    (default ? ListRooms(user)).map {
      case RoomList(list) => list
    }
  }

  def listUsers(roomName: String)(implicit user: User): Future[Set[User]] = {
    (default ? ListUsers(user, roomName)).map {
      case UserList(list) => list
    }
  }

  def listAuthors(roomName: String)(implicit user: User): Future[Set[Author]] = {
    (default ? ListUsers(user, roomName)).map {
      case UserList(list) => list.map(_.author)
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
    case msg@Join(roomName, user) =>
      Logger("chat.server").debug(s"User $user is connecting to room $roomName")
      val room = rooms.get(roomName).getOrElse {
        val room = createRoom(roomName)
        rooms = rooms + (roomName -> room)
        room
      }
      forward(room, msg, sender)
    case ListRooms(user) =>
      Logger("chat.server").debug(s"User $user is listing rooms")
      sender ! RoomList(rooms.keys.toSeq)
    case msg@ListUsers(user, roomName) =>
      Logger("chat.server").debug(s"User $user is listing users on room $roomName")
      rooms.get(roomName) match {
        case Some(room) => forward(room, msg, sender)
        case None => sender ! UserList(Set.empty)
      }
  }

  def forward(target: ActorRef, msg: Any, sender: ActorRef) = {
    Logger("chat.server").debug(s"=>: $target ! $msg")
    (target ? msg).map{ resp =>
      Logger("chat.server").debug(s"<= $sender ! $resp")
      sender ! resp
    }
  }

}

class ChatRoom(roomName: String) extends Actor {

  implicit val timeout = Timeout(5 second)

  var members = Set.empty[User]
  val (chatEnumerator, chatChannel) = Concurrent.broadcast[JsValue]

  def receive = {

    case Join(_, user) => {
      if(members.contains(user)) {
        sender ! CannotConnect("You are already in this room")
      } else {
        members = members + user
        sender ! Connected(self, chatEnumerator)
        self ! NotifyJoin(user)
      }
    }

    case NotifyJoin(user) => {
      implicit val u = user
      notifyAll(Message("info", roomName, "has entered the room"))(user)
    }

    case Talk(user, text) => {
      implicit val u = user
      notifyAll(Message("message", roomName, text))(user)
    }

    case Quit(user) => {
      implicit val u = user
      members = members - user
      notifyAll(Message("info", roomName, "has left the room"))(user)
    }

    case ListUsers(user, _) => {
      Logger("chat.room."+roomName).debug(s"Listing users")
      sender ! UserList(members)
    }

  }

  def notifyAll(msg: Message)(implicit user: User) {
    Logger("chat.room."+roomName).debug(s"notifyAll($msg)($user)")
    implicit val format = MessageJsonFormat
    Message.insert(msg)
    chatChannel.push(Json.toJson(msg))
  }

}

case class Join(roomName: String, user: User)
case class Quit(user: User)
case class Talk(user: User, text: String)
case class NotifyJoin(user: User)
case class ListUsers(user: User, roomName: String)
case class UserList(users: Set[User])

case class ListRooms(user: User)
case class RoomList(rooms: Seq[String])
case class Connected(roomName: Room, enumerator:Enumerator[JsValue])
case class CannotConnect(msg: String)
