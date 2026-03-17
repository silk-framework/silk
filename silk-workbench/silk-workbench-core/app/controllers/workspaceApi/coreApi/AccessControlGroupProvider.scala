package controllers.workspaceApi.coreApi

import org.silkframework.config.DefaultConfig
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.annotations.PluginType
import org.silkframework.runtime.plugin.{AnyPlugin, PluginContext, PluginRegistry}
import org.silkframework.workspace.WorkspaceFactory
import play.api.libs.json.{Format, Json}

import java.util.logging.Logger
import scala.collection.mutable
import scala.util.control.NonFatal

@PluginType()
trait AccessControlGroupProvider extends AnyPlugin {
  /** Returns all known access control group. */
  def groups(implicit userContext: UserContext): Seq[AccessControlGroup]
}

/** An access control group. */
case class AccessControlGroup(id: String)

object AccessControlGroup {
  implicit val accessControlGroupProviderFormat: Format[AccessControlGroup] = Json.format[AccessControlGroup]
}

case class DefaultAccessControlProvider() extends AccessControlGroupProvider {

  /** Returns all known access control group. */
  override def groups(implicit userContext: UserContext): Seq[AccessControlGroup] = {
    val allGroups = new mutable.HashSet[String]()
    WorkspaceFactory().workspace.allProjects.foreach(project => {
      allGroups.addAll(project.accessControl.getGroups)
    })
    allGroups.toSeq.sorted.map(group => AccessControlGroup(group))
  }
}

object AccessControlGroupProvider {
  val log: Logger = Logger.getLogger(this.getClass.getName)
  private final val ACCESS_CONTROL_PROVIDER_PARAMETER_KEY = "workspace.accessControl.groupProvider.plugin"
  // Needed for reload logic, e.g. when tests configure the plugin differently for a specific test suite
  private var lastPlugin: String = ""
  private var accessControlGroupProvider: Option[AccessControlGroupProvider] = None

  private def instance: AccessControlGroupProvider = this.synchronized {
    val config = DefaultConfig.instance()
    try {
      if (config.hasPath(ACCESS_CONTROL_PROVIDER_PARAMETER_KEY)) {
        val accessControlGroupProviderPluginId = config.getString(ACCESS_CONTROL_PROVIDER_PARAMETER_KEY)
        if(accessControlGroupProviderPluginId != lastPlugin || accessControlGroupProvider.isEmpty) {
          implicit val context: PluginContext = PluginContext.empty
          accessControlGroupProvider = Some(PluginRegistry.create[AccessControlGroupProvider](accessControlGroupProviderPluginId))
        }
        lastPlugin = accessControlGroupProviderPluginId
        accessControlGroupProvider.get
      } else {
        DefaultAccessControlProvider()
      }
    } catch {
      case NonFatal(ex) =>
        log.warning("Got exception when creating an access control provider plugin. Exception message: " + ex.getMessage)
        DefaultAccessControlProvider()
    }
  }

  def apply(): AccessControlGroupProvider = instance
}