package controllers.core

import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.users.WebUserManager
import play.api.mvc._

import scala.concurrent.Future

/**
  * Helper method to create actions with user context provided
  */
trait UserContextActions {
  this: BaseController =>

  def UserContextAction(block: (UserContext) => Result): Action[AnyContent] = {
    Action { request =>
      val userContext: UserContext = WebUserManager().userContext(request)
      block(userContext)
    }
  }

  def UserContextAction[A](bodyParser: BodyParser[A])
              (block: (UserContext) => Result): Action[A] = {
    Action(bodyParser) { request =>
      val userContext: UserContext = WebUserManager().userContext(request)
      block(userContext)
    }
  }

  def RequestUserContextAction(block: Request[AnyContent] => UserContext => Result): Action[AnyContent] = {
    this.Action { request =>
      val userContext: UserContext = WebUserManager().userContext(request)
      block(request)(userContext)
    }
  }

  def RequestUserContextAction[A](bodyParser: BodyParser[A])
              (block: Request[A] => UserContext => Result): Action[A] = {
    this.Action(bodyParser) { request =>
      val userContext: UserContext = WebUserManager().userContext(request)
      block(request)(userContext)
    }
  }

  /** Asynchronous variant of [[RequestUserContextAction]]. The block returns a [[Future]] of the result, e.g. for
    * controllers that forward to another service and complete the request once the upstream call has finished. */
  def RequestUserContextActionAsync(block: Request[AnyContent] => UserContext => Future[Result]): Action[AnyContent] = {
    this.Action.async { request =>
      val userContext: UserContext = WebUserManager().userContext(request)
      block(request)(userContext)
    }
  }

  /** Asynchronous variant of [[RequestUserContextAction]] with a custom body parser. */
  def RequestUserContextActionAsync[A](bodyParser: BodyParser[A])
              (block: Request[A] => UserContext => Future[Result]): Action[A] = {
    this.Action.async(bodyParser) { request =>
      val userContext: UserContext = WebUserManager().userContext(request)
      block(request)(userContext)
    }
  }
}