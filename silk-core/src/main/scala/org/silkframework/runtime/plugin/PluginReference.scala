package org.silkframework.runtime.plugin

import org.silkframework.util.Identifier

/**
 * Reference to a plugin.
 *
 * @param id The identifier of the referenced plugin.
 * @param description An optional description of the relationship to the referenced plugin.
 */
case class PluginReference(id: Identifier, description: Option[String])
