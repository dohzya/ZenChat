package models

import play.api.Play.current
import play.api._
import play.api.libs.json._
import play.modules.reactivemongo._
import play.modules.reactivemongo.PlayBsonImplicits._
import reactivemongo.api._
import reactivemongo.bson._
import reactivemongo.bson.handlers.DefaultBSONHandlers._
import reactivemongo.bson.handlers._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._

case class User(
  id: String,
  email: String,
  verifiedEmail: Boolean,
  name: String,
  givenName: String,
  familyName: String,
  link: String,
  picture: String,
  gender: String,
  birthday: String,
  locale: String
)

object User {
  val db = ReactiveMongoPlugin.db
  lazy val collection = db("users")

  def findById(id: String): Future[Option[User]] = {
    val query = BSONDocument("id" -> BSONString(id))
    implicit val handler = UserBsonHandler
    collection.find[User](QueryBuilder(queryDoc = Some(query))).headOption
  }

  def findByName(name: String): Future[Option[User]] = {
    val query = BSONDocument("name" -> BSONString(name))
    implicit val handler = UserBsonHandler
    collection.find[User](QueryBuilder(queryDoc = Some(query))).headOption
  }

}

object UserBsonHandler extends BSONReader[User] with BSONWriter[User] {
  def fromBSON(document: BSONDocument): User = {
    val doc = document.toTraversable
    User(
      id = doc.getAs[BSONString]("id").get.value,
      email = doc.getAs[BSONString]("email").get.value,
      verifiedEmail = doc.getAs[BSONBoolean]("verifiedEmail").get.value,
      name = doc.getAs[BSONString]("name").get.value,
      givenName = doc.getAs[BSONString]("givenName").get.value,
      familyName = doc.getAs[BSONString]("familyName").get.value,
      link = doc.getAs[BSONString]("link").get.value,
      picture = doc.getAs[BSONString]("picture").get.value,
      gender = doc.getAs[BSONString]("gender").get.value,
      birthday = doc.getAs[BSONString]("birthday").get.value,
      locale = doc.getAs[BSONString]("locale").get.value
    )
  }
  def toBSON(o: User): BSONDocument = {
    BSONDocument(
      "id" -> BSONString(o.id),
      "email" -> BSONString(o.email),
      "verifiedEmail" -> BSONBoolean(o.verifiedEmail),
      "name" -> BSONString(o.name),
      "givenName" -> BSONString(o.givenName),
      "familyName" -> BSONString(o.familyName),
      "link" -> BSONString(o.link),
      "picture" -> BSONString(o.picture),
      "gender" -> BSONString(o.gender),
      "birthday" -> BSONString(o.birthday),
      "locale" -> BSONString(o.locale)
    )
  }
}

object UserJsonFormat extends Format[User] {
  def reads(json: JsValue) = JsSuccess(User(
    id = (json \ "id").as[String],
    email = (json \ "email").as[String],
    verifiedEmail = (json \ "verifiedEmail").as[Boolean],
    name = (json \ "name").as[String],
    givenName = (json \ "givenName").as[String],
    familyName = (json \ "familyName").as[String],
    link = (json \ "link").as[String],
    picture = (json \ "picture").as[String],
    gender = (json \ "gender").as[String],
    birthday = (json \ "birthday").as[String],
    locale = (json \ "locale").as[String]
  ))
  def writes(o: User): JsValue = Json.obj(
    "id" -> o.id,
    "email" -> o.email,
    "verifiedEmail" -> o.verifiedEmail,
    "name" -> o.name,
    "givenName" -> o.givenName,
    "familyName" -> o.familyName,
    "link" -> o.link,
    "picture" -> o.picture,
    "gender" -> o.gender,
    "birthday" -> o.birthday,
    "locale" -> o.locale
  )
}
