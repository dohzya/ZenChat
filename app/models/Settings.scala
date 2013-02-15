package models

import play.api._
import reactivemongo.bson._
import reactivemongo.bson.handlers._

case class Settings(
)

object SettingsBsonHandler extends BSONReader[Settings] with BSONWriter[Settings] {
  def fromBSON(document: BSONDocument): Settings = {
    val doc = document.toTraversable
    Settings(
    )
  }
  def toBSON(o: Settings): BSONDocument = {
    BSONDocument(
    )
  }
}
