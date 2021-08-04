package controllers.core

import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.users.WebUserManager
import play.api.mvc._

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
}