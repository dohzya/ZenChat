import play.api._
import scala.concurrent.ExecutionContext.Implicits.global

import models._

object Global extends GlobalSettings {

  override def onStart(app: Application) {
    for (index <- User.indexes) User.collection.indexesManager.ensure(index)
  }

}
