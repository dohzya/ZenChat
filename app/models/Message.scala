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
  author: User,
  text: String,
  date: DateTime = DateTime.now
)
object Message {
  val db = ReactiveMongoPlugin.db
  lazy val collection = db("messages")
  collection.createCapped(size = 100L, maxDocuments = Some(1000))

  def apply(text: String)(implicit author: User): Message = {
    Message(
      author = author,
      text = text
    )
  }

  def create(text: String)(implicit author: User): Future[Try[Unit]] = {
    insert(Message(text))
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
      author = UserBsonHandler.fromBSON(doc.getAs[BSONDocument]("author").get),
      text = doc.getAs[BSONString]("text").get.value,
      date = new DateTime(doc.getAs[BSONDateTime]("date").get.value)
    )
  }
  def toBSON(o: Message): BSONDocument = {
    BSONDocument(
      "_id" -> o.id,
      "author" -> UserBsonHandler.toBSON(o.author),
      "text" -> BSONString(o.text),
      "date" -> BSONDateTime(o.date.getMillis)
    )
  }
}

object MessageJsonFormat extends Format[Message] {
  def reads(json: JsValue) = UserJsonFormat.reads(json \ "author").flatMap { author =>
    JsSuccess(Message(
      id = BSONObjectID((json \ "id").as[String]),
      author = author,
      text = (json \ "text").as[String],
      date = (json \ "date").as[DateTime]
    ))
  }
  def writes(o: Message): JsValue = Json.obj(
    "id" -> o.id.stringify,
    "author" -> UserJsonFormat.writes(o.author),
    "text" -> o.text,
    "date" -> Json.toJson(o.date)
  )
}
