package controllers.api.nodes

import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import controllers.Application
import controllers.api.router.{DefaultNestedResourceController, NestedResourceRouter}
import model.WustSchema._
import modules.auth.HeaderEnvironmentModule
import modules.requests.{ConnectSchema, HyperConnectSchema, NodeSchema}
import play.api.libs.json.JsValue
import play.api.mvc.Results._
import play.api.mvc.{AnyContent, Controller, Result}
import renesca.schema._

case class RequestContext(controller: NodesBase with Controller, user: Option[User], json: Option[JsValue], query: Map[String, String]) {
  def page = query.get("page").map(_.toInt)
  def size = query.get("size").map(_.toInt)
  def skip = query.get("skip").map(_.toInt)
  def sizeWithDefault = size.getOrElse(15)

  def scopes = query.get("scopes").map(_.split(",").toSeq).getOrElse(Seq.empty)

  def jsonAs[T](implicit rds: play.api.libs.json.Reads[T]) = {
    json.flatMap(_.validate[T].asOpt)
  }

  def withJson[S](handler: S => Result)(implicit rds: play.api.libs.json.Reads[S]) = {
    jsonAs[S].map(handler(_)).getOrElse(UnprocessableEntity("Cannot parse json body"))
  }

  def withUser(handler: User => Result): Result = {
    user.map(handler(_)).getOrElse(onlyUsersError)
  }

  def withUser(handler: => Result): Result = {
    user.map(_ => handler).getOrElse(onlyUsersError)
  }

  def onlyUsersError = Forbidden("Only for users")
}

case class ConnectParameter[+BASE <: UuidNode](
  factory: UuidNodeMatchesFactory[BASE],
  baseUuid: String
  )

trait HyperConnectParameter[START <: UuidNode, RELATION <: UuidNode with HyperRelation[START, _ <: Relation[START,RELATION], RELATION, _ <: Relation[RELATION,END], END], END <: UuidNode] {
  def baseUuid: String
  val startFactory: UuidNodeMatchesFactory[START]
  val startUuid: String
  val factory: MatchableRelationFactory[START, RELATION, END] with UuidNodeMatchesFactory[RELATION] with HyperRelationFactory[START,_ <: Relation[START,RELATION],RELATION,_ <: Relation[RELATION,END],END]
  val endFactory: UuidNodeMatchesFactory[END]
  val endUuid: String
}

case class StartHyperConnectParameter[START <: UuidNode, RELATION <: UuidNode with HyperRelation[START, _ <: Relation[START,RELATION], RELATION, _ <: Relation[RELATION,END], END], END <: UuidNode](
  startFactory: UuidNodeMatchesFactory[START],
  startUuid: String,
  factory: MatchableRelationFactory[START, RELATION, END] with UuidNodeMatchesFactory[RELATION] with HyperRelationFactory[START,_ <: Relation[START,RELATION],RELATION,_ <: Relation[RELATION,END],END],
  endFactory: UuidNodeMatchesFactory[END],
  endUuid: String
  ) extends HyperConnectParameter[START, RELATION, END] {
    def baseUuid = startUuid
}

case class EndHyperConnectParameter[START <: UuidNode, RELATION <: UuidNode with HyperRelation[START, _ <: Relation[START,RELATION], RELATION, _ <: Relation[RELATION,END], END], END <: UuidNode](
  endFactory: UuidNodeMatchesFactory[END],
  endUuid: String,
  factory: MatchableRelationFactory[START, RELATION, END] with UuidNodeMatchesFactory[RELATION] with HyperRelationFactory[START,_ <: Relation[START,RELATION],RELATION,_ <: Relation[RELATION,END],END],
  startFactory: UuidNodeMatchesFactory[START],
  startUuid: String
  ) extends HyperConnectParameter[START, RELATION, END] {
    def baseUuid = endUuid
}
