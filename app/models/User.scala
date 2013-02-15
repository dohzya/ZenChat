package models

import org.joda.time._
import play.api.Play.current
import play.api._
import play.api.libs.json._
import play.modules.reactivemongo._
import play.modules.reactivemongo.PlayBsonImplicits._
import reactivemongo.api._
import reactivemongo.bson._
import reactivemongo.bson.handlers.DefaultBSONHandlers._
import reactivemongo.bson.handlers._
import reactivemongo.core.commands.LastError
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._

case class User(
  id: BSONObjectID,
  author: Author,
  settings: Settings,
  createdAt: DateTime,
  lastActivityAt : DateTime
)

object User {
  val db = ReactiveMongoPlugin.db
  lazy val collection = db("users")

  def indexes = Seq(
    reactivemongo.api.indexes.Index(
      key = List("author.id" -> reactivemongo.api.indexes.IndexType.Ascending),
      unique = true
    )
  )

  def create(author: Author) = {
    User(
      id = BSONObjectID.generate,
      author = author,
      settings = Settings(),
      createdAt = DateTime.now,
      lastActivityAt = DateTime.now
    )
  }

  def merge(oldUser: User, newUser: User): Future[User] = {
    val mergedUser = newUser.copy(
      settings = oldUser.settings,
      createdAt = oldUser.createdAt,
      lastActivityAt = oldUser.lastActivityAt
    )
    Future { mergedUser }  // FIXME
  }

  def createOrMerge(author: Author): Future[User] = {
    implicit val handler = UserBsonHandler
    val newUser = create(author)
    findByAuthorId(author.id).flatMap {
      case Some(oldUser) => merge(oldUser, newUser)
      case None =>
        collection.insert(newUser)
                  .map{ _ => newUser }
                  .recoverWith {
                    case l: LastError if l.code == Some(11000) =>
                      findByAuthorId(author.id).flatMap{ oldUser => merge(oldUser.get, newUser) }
                  }
    }
  }

  def findById(id: String): Future[Option[User]] = {
    val query = BSONDocument("_id" -> BSONObjectID(id))
    implicit val handler = UserBsonHandler
    collection.find[User](QueryBuilder(queryDoc = Some(query))).headOption
  }

  def findByAuthorId(id: String): Future[Option[User]] = {
    val query = BSONDocument("author.id" -> BSONString(id))
    implicit val handler = UserBsonHandler
    collection.find[User](QueryBuilder(queryDoc = Some(query))).headOption
  }

  def findByName(name: String): Future[Option[User]] = {
    val query = BSONDocument("author.name" -> BSONString(name))
    implicit val handler = UserBsonHandler
    collection.find[User](QueryBuilder(queryDoc = Some(query))).headOption
  }

}

object UserBsonHandler extends BSONReader[User] with BSONWriter[User] {
  def fromBSON(document: BSONDocument): User = {
    val doc = document.toTraversable
    User(
      id = doc.getAs[BSONObjectID]("_id").get,
      author = AuthorBsonHandler.fromBSON(doc.getAs[BSONDocument]("author").get),
      settings = doc.getAs[BSONDocument]("settings").map(SettingsBsonHandler.fromBSON(_)).getOrElse(Settings()),
      createdAt = new DateTime(doc.getAs[BSONDateTime]("createdAt").get.value),
      lastActivityAt = new DateTime(doc.getAs[BSONDateTime]("lastActivityAt").get.value)
    )
  }
  def toBSON(o: User): BSONDocument = {
    BSONDocument(
      "_id" -> o.id,
      "author" -> AuthorBsonHandler.toBSON(o.author),
      "settings" -> SettingsBsonHandler.toBSON(o.settings),
      "createdAt" -> BSONDateTime(o.createdAt.getMillis),
      "lastActivityAt" -> BSONDateTime(o.lastActivityAt.getMillis)
    )
  }
}

object UserJsonWrite extends Writes[User] {
  def writes(o: User): JsValue = Json.obj(
    "id" -> o.id.stringify,
    "author" -> AuthorJsonFormat.writes(o.author),
    "createdAt" -> Json.toJson(o.createdAt),
    "lastActivityAt" -> Json.toJson(o.lastActivityAt)
  )
}
