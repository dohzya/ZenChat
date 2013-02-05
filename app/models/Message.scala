package models

import org.joda.time._
import play.api.Play.current
import play.api._
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
    collection.insert(msg).map{ r => if (r.ok) Success(r) else Failure(r) }
  }

  def all: Future[Seq[Message]] = {
    val query = BSONDocument()
    implicit val handler = MessageBsonHandler
    collection.find[Message](QueryBuilder(queryDoc = Some(query))).toList
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
