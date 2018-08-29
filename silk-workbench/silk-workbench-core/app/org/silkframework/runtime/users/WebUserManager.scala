package org.silkframework.runtime.users

import java.util.logging.Logger

import org.silkframework.config.{DefaultConfig, Prefixes}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.PluginRegistry
import org.silkframework.runtime.resource.{EmptyResourceManager, ResourceManager}
import play.api.mvc.RequestHeader

import scala.util.control.NonFatal

/**
  * Fetches user related data from a request object.
  */
trait WebUserManager {
  def user(request: RequestHeader): Option[WebUser]

  def userContext(request: RequestHeader): UserContext
}

object WebUserManager {
  val log: Logger = Logger.getLogger(this.getClass.getName)
  final val WEB_USER_MANAGER_PARAMETER_KEY = "user.manager.web.plugin"
  private var lastPlugin: String = ""
  private var webUserManager: Option[WebUserManager] = None

  def instance: WebUserManager = this.synchronized {
    val config = DefaultConfig.instance()
    try {
      if (config.hasPath(WEB_USER_MANAGER_PARAMETER_KEY)) {
        val userManagerPluginId = config.getString(WEB_USER_MANAGER_PARAMETER_KEY)
        if(userManagerPluginId != lastPlugin || webUserManager.isEmpty) {
          implicit val prefixes: Prefixes = Prefixes.empty
          implicit val resourceManager: ResourceManager = EmptyResourceManager
          webUserManager = Some(PluginRegistry.create[WebUserManager](userManagerPluginId))
        }
        lastPlugin = userManagerPluginId
        webUserManager.get
      } else {
        EmptyWebUserManager
      }
    } catch {
      case NonFatal(ex) =>
        log.warning("Got exception when creating web user manager plugin. Exception message: " + ex.getMessage)
        EmptyWebUserManager
    }
  }

  def apply(): WebUserManager = instance
}

object EmptyWebUserManager extends WebUserManager {
  override def user(request: RequestHeader): Option[WebUser] = None

  override def userContext(request: RequestHeader): UserContext = UserContext.Empty
}