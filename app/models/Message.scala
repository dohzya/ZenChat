package models

import org.joda.time._
import play.api.Play.current
import play.api._
import play.api.libs.iteratee._
import play.api.libs.json._
import play.modules.reactivemongo.PlayBsonImplicits._
import play.modules.reactivemongo._
import reactivemongo.api._
import reactivemongo.bson._
import reactivemongo.bson.handlers.DefaultBSONHandlers._
import reactivemongo.bson.handlers._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.util._

case class Message(
  id: BSONObjectID = BSONObjectID.generate,
  _type: String,
  author: Author,
  roomName: String,
  text: String,
  date: DateTime = DateTime.now
)
object Message {
  val db = ReactiveMongoPlugin.db
  lazy val collection = db("messages")
  collection.createCapped(size = 100L, maxDocuments = Some(1000))

  def apply(_type: String, roomName: String, text: String)(implicit user: User): Message = {
    Message(
      _type = _type,
      roomName = roomName,
      author = user.author,
      text = text
    )
  }

  def insert(msg: Message): Future[Try[Unit]] = {
    implicit val handler = MessageBsonHandler
    collection.insert(msg).map{ res =>
      if (res.ok) Success(()) else Failure(res)
    }
  }

  def all: Future[Seq[Message]] = {
    val query = BSONDocument()
    implicit val handler = MessageBsonHandler
    collection.find[Message](QueryBuilder().query(query)).toList
  }
  def all(roomName: String): Future[Seq[Message]] = {
    val query = BSONDocument("roomName" -> BSONString(roomName))
    implicit val handler = MessageBsonHandler
    collection.find[Message](QueryBuilder().query(query)).toList
  }

  def enumerate: Enumerator[Message] = {
    val query = BSONDocument()
    implicit val handler = MessageBsonHandler
    collection.find[Message](QueryBuilder().query(query), QueryOpts().tailable.awaitData).enumerate
  }

}

object MessageBsonHandler extends BSONReader[Message] with BSONWriter[Message] {
  def fromBSON(document: BSONDocument): Message = {
    val doc = document.toTraversable
    Message(
      id = doc.getAs[BSONObjectID]("_id").get,
      _type = doc.getAs[BSONString]("_type").map(_.value).getOrElse("message"),
      author = AuthorBsonHandler.fromBSON(doc.getAs[BSONDocument]("author").get),
      roomName = doc.getAs[BSONString]("roomName").get.value,
      text = doc.getAs[BSONString]("text").get.value,
      date = new DateTime(doc.getAs[BSONDateTime]("date").get.value)
    )
  }
  def toBSON(o: Message): BSONDocument = {
    BSONDocument(
      "_id" -> o.id,
      "_type" -> BSONString(o._type),
      "author" -> AuthorBsonHandler.toBSON(o.author),
      "roomName" -> BSONString(o.roomName),
      "text" -> BSONString(o.text),
      "date" -> BSONDateTime(o.date.getMillis)
    )
  }
}

object MessageJsonFormat extends Format[Message] {
  def reads(json: JsValue) = AuthorJsonFormat.reads(json \ "author").flatMap { author =>
    JsSuccess(Message(
      id = BSONObjectID((json \ "id").as[String]),
      _type = (json \ "type").as[String],
      author = author,
      roomName = (json \ "roomName").as[String],
      text = (json \ "text").as[String],
      date = (json \ "date").as[DateTime]
    ))
  }
  def writes(o: Message): JsValue = Json.obj(
    "id" -> o.id.stringify,
    "type" -> o._type,
    "author" -> AuthorJsonFormat.writes(o.author),
    "roomName" -> o.roomName,
    "text" -> o.text,
    "date" -> Json.toJson(o.date)
  )
}
