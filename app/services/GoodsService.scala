package services

import java.sql.Connection

import anorm._
import models.Goods
import play.api.db.DB
import play.api.Play.current

import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future

class GoodsService {

  def add(goods: Goods): Future[Option[Goods]] = {
    checkNotExists(goods).flatMap {
      case true => Future {
        DB.withTransaction { implicit connection =>
          SQL"INSERT INTO goods(name, price) VALUES (${goods.name}, ${goods.price})".executeInsert[Option[Long]]().map(id => goods.copy(id = id))
        }
      }
      case false => Future.successful(None)
    }
  }

  def update(goods: Goods): Future[Boolean] = {
    checkNotExists(goods).flatMap {
      case true => Future {
        DB.withTransaction { implicit connection =>
          SQL"UPDATE goods SET name = ${goods.name}, price = ${goods.price} WHERE id = ${goods.id}".executeUpdate() > 0
        }
      }
      case false => Future.successful(false)
    }
  }

  def delete(goods: Goods): Future[Boolean] = {
    Future {
      DB.withTransaction { implicit connection =>
        SQL"DELETE FROM goods WHERE id = ${goods.id}".executeUpdate() > 0
      }
    }
  }

  def search(nameOpt: Option[String]): Future[List[Goods]] = {

    def allGoods(implicit connection: Connection): List[Goods] = SQL"SELECT g.id, g.name, g.price FROM goods g ORDER by g.name".as(goodsParser.*)

    Future {
      DB.withTransaction { implicit connection =>
        if (nameOpt.isDefined) {
          val trimmedName: String = nameOpt.get.trim
          if (!trimmedName.isEmpty) {
            SQL"SELECT g.id, g.name, g.price FROM goods g WHERE g.name like ${trimmedName + "%"} ORDER by g.name".as(goodsParser.*)
          }
          else {
            allGoods
          }
        }
        else {
          allGoods
        }
      }
    }
  }

  protected def checkNotExists(goods: Goods): Future[Boolean] = {
    Future {
      DB.withConnection { implicit connection =>
        SQL"SELECT count(*) FROM goods g WHERE g.name = ${goods.name} and g.price = ${goods.price}".as(SqlParser.scalar[Long].single) == 0
      }
    }
  }

  protected def findByGoods(name: String, price: Long)(implicit connection: Connection): List[Goods] = {
    SQL"SELECT g.id, g.name, g.price FROM goods g WHERE g.name = $name and g.price = $price".apply().map (row =>
      Goods(row[Long]("id"), row[String]("name"), row[Long]("price"))
    ).toList
  }

  protected val goodsParser = {
    import anorm.SqlParser._
    long("id") ~ str("name") ~ long("price") map {
      case i~n~p => Goods(i, n, p)
    }
  }
}
