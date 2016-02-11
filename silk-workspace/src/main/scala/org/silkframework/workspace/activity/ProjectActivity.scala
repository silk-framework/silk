package org.silkframework.workspace.activity

import org.silkframework.runtime.activity.{Activity, ActivityControl}
import org.silkframework.runtime.plugin.PluginDescription
import org.silkframework.workspace.Project

class ProjectActivity(val project: Project, initialFactory: ProjectActivityFactory[_]) {

  @volatile
  private var currentControl: ActivityControl[_] = Activity(initialFactory(project))

  @volatile
  private var currentFactory = initialFactory

  def name = currentControl.name

  def value = currentControl.value()

  def status = currentControl.status()

  def control = currentControl

  def factory = currentFactory

  def config: Map[String, String] = PluginDescription(currentFactory.getClass).parameterValues(currentFactory)

  def update(config: Map[String, String]) = {
    val oldControl = currentControl
    currentFactory = PluginDescription(currentFactory.getClass)(config)
    currentControl = Activity(currentFactory(project))
    // Keep subscribers
    for(subscriber <- oldControl.status.subscribers) {
      currentControl.status.onUpdate(subscriber)
    }
  }

  def activityType = currentFactory.activityType
}