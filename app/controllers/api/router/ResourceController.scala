// https://gist.github.com/jroper/5533633

package controllers.api.router

import common.Helpers.compose
import play.api.mvc._
import play.core.Router

import scala.runtime.AbstractPartialFunction

trait ResourceController extends Controller {

  /**
   * Get the index for the resource.
   *
   * Typically this might be a list of interesting resources.
   *
   * This is invoked for GET requests on /myresource.
   */
  def index: EssentialAction

  /**
   * Create a new resource.
   *
   * This is invoked for POST requests on /myresource.
   */
  def create: EssentialAction

  /**
   * Get a given resource.
   *
   * This is invoked for GET requests on /myresource/id.
   *
   * @param id The id of the resource to get.
   */
  def show(id: String): EssentialAction

  /**
   * Update the resource with the given id.
   *
   * This is invoked for PUT requests on /myresource/id.
   *
   * @param id The id of the resource to update.
   */
  def update(id: String): EssentialAction

  /**
   * Delete the resource with the given id.
   *
   * This is invoked for DELETE requests on /myresource/id
   *
   * @param id The id of the resource to delete.
   */
  def destroy(id: String): EssentialAction
}

trait DefaultResourceController extends ResourceController {
  override def index: EssentialAction = ???
  override def create: EssentialAction = ???
  override def show(id: String): EssentialAction = ???
  override def update(id: String): EssentialAction = ???
  override def destroy(id: String): EssentialAction = ???
}

/**
 * A resource router. Typically it will be implemented by an object, like so:
 *
 * {{{
 *   object MyResource extends ResourceRouter[Long] {
 *     def index = Action {
 *       ...
 *     }
 *     def create(id: Long) = Action {
 *       ...
 *     }
 *
 *     ...
 *   }
 * }}}
 *
 * Then from a routes file can be used like so:
 *
 * {{{
 *   ->   /myresource     controllers.MyResource
 * }}}
 *
 * @param name A custom name for the resource controller.  This is used by the Javascript router.
 * @param idBindable The bindable for converting the id to the expected type.
 * @tparam T
 */
trait ResourceRouter extends Router.Routes with ResourceController {
  private var path: String = ""

  protected val pathElement = "/([^/]+)"
  private val MaybeSlash = "/?".r
  private val Id = pathElement.r

  private val mapRequestToAction: PartialFunction[(String, String), EssentialAction] = {
    case ("GET", MaybeSlash())  => index
    case ("POST", MaybeSlash()) => create
    case ("GET", Id(id))        => show(id)
    case ("PUT", Id(id))        => update(id)
    case ("DELETE", Id(id))     => destroy(id)
  }

  private val isOwnRoute: PartialFunction[(String, String), (String, String)] = {
    case (method, url) if url.startsWith(path) => (method, url.drop(path.length))
  }

  protected def mapRequest = mapRequestToAction

  private def mapOwnRequestToAction: PartialFunction[(String, String), EssentialAction] = compose(isOwnRoute, mapRequest)

  def routes = new AbstractPartialFunction[RequestHeader, Handler] {
    override def applyOrElse[A <: RequestHeader, B >: Handler](rh: A, default: A => B) = {
      mapOwnRequestToAction.lift((rh.method, rh.path)).getOrElse(default(rh))
    }

    def isDefinedAt(rh: RequestHeader) = mapOwnRequestToAction.isDefinedAt((rh.method, rh.path))
  }

  def setPrefix(prefix: String) {
    path = prefix
  }

  def prefix = path

  // TODO: documentation
  def documentation = Seq()
}
