package org.silkframework.workspace.activity

import org.silkframework.config.Prefixes
`import org.silkframework.runtime.activity.{Activity, ActivityControl, HasValue, UserContext}
import org.silkframework.runtime.plugin.PluginDescription
import org.silkframework.util.Identifier
import org.silkframework.workspace.Project

import scala.reflect.ClassTag

class ProjectActivity[ActivityType <: HasValue : ClassTag](override val project: Project, initialFactory: ProjectActivityFactory[ActivityType])
  extends WorkspaceActivity[ActivityType] {

  @volatile
  private var currentControl: ActivityControl[ActivityType#ValueType] = Activity(initialFactory(project))

  @volatile
  private var currentFactory = initialFactory

  override def name = currentFactory.pluginSpec.id

  override def taskOption = None

  override def control = currentControl

  def factory = currentFactory

  def config: Map[String, String] = PluginDescription(currentFactory.getClass).parameterValues(currentFactory)(Prefixes.empty)

  def update(config: Map[String, String]): Unit = {
    val oldControl = currentControl
    implicit val prefixes = project.config.prefixes
    implicit val resources = project.resources
    currentFactory = PluginDescription(currentFactory.getClass)(config)
    currentControl = Activity(currentFactory(project))
    // Keep subscribers
    for(subscriber <- oldControl.status.subscribers) {
      currentControl.status.subscribe(subscriber)
    }
  }

  def activityType = currentFactory.activityType

  /**
    * Starts the activity asynchronously.
    * Optionally applies a supplied configuration.
    */
  def start(config: Map[String, String] = Map.empty)(implicit user: UserContext = UserContext.Empty): Identifier = {
    control.start()
    name
  }

  /**
    * Starts the activity blocking.
    * Optionally applies a supplied configuration.
    */
  def startBlocking(config: Map[String, String] = Map.empty)(implicit user: UserContext = UserContext.Empty): Identifier = {
    control.startBlocking()
    name
  }
}