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

case class Author(
  id: String,
  email: String,
  verifiedEmail: Boolean,
  name: String,
  givenName: String,
  familyName: String,
  link: String,
  picture: Option[String],
  gender: String,
  birthday: Option[String],
  locale: Option[String]
)

object AuthorBsonHandler extends BSONReader[Author] with BSONWriter[Author] {
  def fromBSON(document: BSONDocument): Author = {
    val doc = document.toTraversable
    Author(
      id = doc.getAs[BSONString]("id").get.value,
      email = doc.getAs[BSONString]("email").get.value,
      verifiedEmail = doc.getAs[BSONBoolean]("verifiedEmail").get.value,
      name = doc.getAs[BSONString]("name").get.value,
      givenName = doc.getAs[BSONString]("givenName").get.value,
      familyName = doc.getAs[BSONString]("familyName").get.value,
      link = doc.getAs[BSONString]("link").get.value,
      picture = doc.getAs[BSONString]("picture").map(_.value),
      gender = doc.getAs[BSONString]("gender").get.value,
      birthday = doc.getAs[BSONString]("birthday").map(_.value),
      locale = doc.getAs[BSONString]("locale").map(_.value)
    )
  }
  def toBSON(o: Author): BSONDocument = {
    BSONDocument(
      "id" -> BSONString(o.id),
      "email" -> BSONString(o.email),
      "verifiedEmail" -> BSONBoolean(o.verifiedEmail),
      "name" -> BSONString(o.name),
      "givenName" -> BSONString(o.givenName),
      "familyName" -> BSONString(o.familyName),
      "link" -> BSONString(o.link),
      "picture" -> o.picture.map(BSONString(_)).getOrElse(BSONNull),
      "gender" -> BSONString(o.gender),
      "birthday" -> o.birthday.map(BSONString(_)).getOrElse(BSONNull),
      "locale" -> o.locale.map(BSONString(_)).getOrElse(BSONNull)
    )
  }
}

object AuthorJsonFormat extends Format[Author] {
  def reads(json: JsValue) = JsSuccess(Author(
    id = (json \ "id").as[String],
    email = (json \ "email").as[String],
    verifiedEmail = (json \ "verifiedEmail").as[Boolean],
    name = (json \ "name").as[String],
    givenName = (json \ "givenName").as[String],
    familyName = (json \ "familyName").as[String],
    link = (json \ "link").as[String],
    picture = (json \ "picture").asOpt[String],
    gender = (json \ "gender").as[String],
    birthday = (json \ "birthday").asOpt[String],
    locale = (json \ "locale").asOpt[String]
  ))
  def writes(o: Author): JsValue = Json.obj(
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
