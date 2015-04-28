package controllers

import play.api.mvc._
import play.core.Router
import scala.runtime.AbstractPartialFunction
import scala.reflect.ClassTag
import play.api.Play

trait NestedResourceController {
  def showMembers(path: String, uuid: String): EssentialAction
  def showNestedMembers(path: String, nestedPath: String, uuid: String, otherUuid: String): EssentialAction
  def connectMember(path: String, uuid: String): EssentialAction
  def connectNestedMember(path: String, nestedPath: String, uuid: String, otherUuid: String): EssentialAction
  def disconnectMember(path: String, uuid: String, otherUuid: String): EssentialAction
  def disconnectNestedMember(path: String, nestedPath: String, uuid: String, otherUuid: String, nestedUuid: String): EssentialAction
}

trait NestedResourceRouter extends ResourceRouter with NestedResourceController {
  protected val IdWithPath = (pathElement * 2).r
  protected val IdWithPathAndId = (pathElement * 3).r
  protected val IdWithPathAndIdWithPath = (pathElement * 4).r
  protected val IdWithPathAndIdWithPathAndId = (pathElement * 5).r

  private val mapRequestToAction: PartialFunction[(String,String),EssentialAction] = {
    case ("GET", IdWithPath(id, path)) => showMembers(path, id)
    case ("POST", IdWithPath(id, path)) => connectMember(path, id)
    case ("DELETE", IdWithPathAndId(id, path, otherId)) => disconnectMember(path, id, otherId)
    case ("GET", IdWithPathAndIdWithPath(id, path, otherId, otherPath)) => showNestedMembers(path, otherPath, id, otherId)
    case ("POST", IdWithPathAndIdWithPath(id, path, otherId, otherPath)) => connectNestedMember(path, otherPath, id, otherId)
    case ("DELETE", IdWithPathAndIdWithPathAndId(id, path, otherId, otherPath, nestedId)) => disconnectNestedMember(path, otherPath, id, otherId, nestedId)
  }

  override protected def mapRequest = super.mapRequest orElse mapRequestToAction
}
