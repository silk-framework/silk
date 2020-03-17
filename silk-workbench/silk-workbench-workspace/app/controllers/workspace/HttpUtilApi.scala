package controllers.workspace

import play.api.mvc.{Action, AnyContent, InjectedController}

/**
  *
  */
class HttpUtilApi extends InjectedController {
  /** Permanently redirects from this path to the same path prefixed by prefix. */
  def permPrefixRedirect(prefix: String, param1: String = ""): Action[AnyContent] = Action { request =>
    val originalPath = request.path.stripPrefix("/")
    val p = prefix.dropWhile(_ == '/').stripSuffix("/")
    val queryString = request.rawQueryString
    PermanentRedirect(s"/$p/$originalPath")
  }
}
