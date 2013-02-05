package models

import play.api.Play.current
import play.api._
import play.modules.reactivemongo._
import play.modules.reactivemongo.PlayBsonImplicits._
import reactivemongo.api._
import reactivemongo.bson._
import reactivemongo.bson.handlers.DefaultBSONHandlers._
import reactivemongo.bson.handlers._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._

case class User(name: String)

object User {
  val db = ReactiveMongoPlugin.db
  lazy val collection = db("users")

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
      doc.getAs[BSONString]("name").get.value
    )
  }
  def toBSON(o: User): BSONDocument = {
    BSONDocument(
      "name" -> BSONString(o.name)
    )
  }
}
