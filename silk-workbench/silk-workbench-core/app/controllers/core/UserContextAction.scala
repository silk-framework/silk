package controllers.core

import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.users.WebUserManager
import play.api.mvc._

/**
  * Helper method to create actions with user context provided
  */
object UserContextAction {
  def apply(block: (UserContext) => Result): Action[AnyContent] = {
    Action { request =>
      val userContext: UserContext = WebUserManager().userContext(request)
      block(userContext)
    }
  }
}

object RequestUserContextAction {
  def apply(block: Request[AnyContent] => UserContext => Result): Action[AnyContent] = {
    Action { request =>
      val userContext: UserContext = WebUserManager().userContext(request)
      block(request)(userContext)
    }
  }
}