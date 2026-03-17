package org.silkframework.workspace.access

import com.typesafe.config.Config
import org.silkframework.config.ConfigValue

/**
 * Holds the access control configuration.
 *
 * @param enabled True, if access control is enabled.
 * @param adminAction Users with this action may read and modify all projects.
 */
case class AccessControlConfig(enabled: Boolean, adminAction: String)

/**
 * Current access control configuration.
 */
object AccessControlConfig extends ConfigValue[AccessControlConfig] {

  override protected def load(config: Config): AccessControlConfig = {
    AccessControlConfig(
      enabled = config.getBoolean("workspace.accessControl.enabled"),
      adminAction = config.getString("workspace.accessControl.adminAction")
    )
  }
}