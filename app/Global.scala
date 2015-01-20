import java.nio.file.{Files, Paths, Path}

import play.api.db.DB
import play.api.{Logger, Application, GlobalSettings}
import anorm._

import scala.io.Source.fromInputStream

object Global extends GlobalSettings {

  override def onStart(app: Application) {
    implicit val application = app

    DB.withTransaction { implicit connection =>
      val sql = fromInputStream(getClass.getResourceAsStream("/create.sql")).mkString
      Logger.info(s"Executing sql statement:\n$sql")
      SQL(sql).execute()
    }
  }
}
