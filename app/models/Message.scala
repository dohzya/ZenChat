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
    val docUser = doc.getAs[BSONDocument]("author").get.toTraversable
    Message(
      id = doc.getAs[BSONObjectID]("_id").get,
      author = User(
        docUser.getAs[BSONString]("name").get.value
      ),
      text = doc.getAs[BSONString]("text").get.value,
      date = new DateTime(doc.getAs[BSONDateTime]("date").get.value)
    )
  }
  def toBSON(o: Message): BSONDocument = {
    BSONDocument(
      "_id" -> o.id,
      "author" -> BSONDocument(
        "name" -> BSONString(o.author.name)
      ),
      "text" -> BSONString(o.text),
      "date" -> BSONDateTime(o.date.getMillis)
    )
  }
}

object MessageJsonFormat extends Format[Message] {
  def reads(json: JsValue) = JsSuccess(Message(
    id = BSONObjectID((json \ "id").as[String]),
    author = User(
      name = (json \ "author" \ "name").as[String]
    ),
    text = (json \ "text").as[String],
    date = (json \ "date").as[DateTime]
  ))
  def writes(o: Message): JsValue = Json.obj(
    "id" -> o.id.stringify,
    "author" -> Json.obj(
      "name" -> o.author.name,
      "avatar" -> "http://lorempixel.com/32/32/"
    ),
    "text" -> o.text,
    "date" -> Json.toJson(o.date)
  )
}
