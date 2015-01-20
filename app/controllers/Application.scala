package controllers

import models.Goods
import play.Logger
import play.api.mvc.{Result, BodyParsers, Action, Controller}
import play.api.libs.json._
import play.api.libs.functional.syntax._
import services.GoodsService

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future

object Application extends Controller {

  val goodsService = new GoodsService

  implicit val goodsFormat: Format[Goods] = (
    (JsPath \ "id").formatNullable[Long] and
    (JsPath \ "name").format[String] and
    (JsPath \ "price").format[String]
  )((idOpt, name, price) => Goods(idOpt.getOrElse(0), name, price.toLong), unlift((goods) => if (goods ne null) Some(Some(goods.id), goods.name, goods.price.toString) else None))

  def search(nameOpt: Option[String]) = Action.async {
    goodsService.search(nameOpt).map(goods =>
      Ok(Json.toJson(goods))
    )
  }

  def add = Action.async(BodyParsers.parse.json) { request =>
    parseGoodsAsync(request.body) { goods =>
      goodsService.add(goods).map(goodsOpt =>
        Ok(Json.obj("success" -> goodsOpt.isDefined, "data" -> Json.toJson(goodsOpt.getOrElse(goods))))
      )
    }
  }

  def delete(id: Long) = Action.async(BodyParsers.parse.json) { request =>
    parseGoodsAsync(request.body) { goods =>
      goodsService.delete(goods).map {
        case true => Ok("delete")
        case false => BadRequest("")
      }
    }
  }

  def update(id: Long) = Action.async(BodyParsers.parse.json) { request =>
    parseGoodsAsync(request.body) { goods =>
      goodsService.update(goods).map(updateResult =>
        Ok(Json.obj("success" -> updateResult, "data" -> Json.toJson(goods)))
      )
    }
  }

  protected def parseGoods(jsValue: JsValue)(block: Goods => Result) = {
    val goodsResult = jsValue.validate[Goods]
    goodsResult.fold(
      errors => {
        Logger.warn(s"Can't parse $jsValue to Goods $errors")
        BadRequest(Json.obj("success" -> false, "error" -> JsError.toFlatJson(errors)))
      },
      goods => {
        block(goods)
      })
  }

  protected def parseGoodsAsync(jsValue: JsValue)(block: Goods => Future[Result]) = {
    val goodsResult = jsValue.validate[Goods]
    goodsResult.fold(
      errors => {
        Logger.warn(s"Can't parse $jsValue to Goods $errors")
        Future.successful(BadRequest(Json.obj("success" -> false, "error" -> JsError.toFlatJson(errors))))
      },
      goods => {
        block(goods)
      })
  }
}