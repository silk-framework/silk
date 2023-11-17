package org.silkframework.runtime.plugin

import org.silkframework.config.Prefixes
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.{EmptyResourceManager, ResourceManager}
import org.silkframework.runtime.templating.{GlobalTemplateVariables, TemplateVariablesReader}
import org.silkframework.util.Identifier

/** PluginContext that should be used in tests. */
object TestPluginContext {
  def apply(prefixes: Prefixes = Prefixes.empty,
            resources: ResourceManager = EmptyResourceManager(),
            user: UserContext = UserContext.Empty,
            projectId: Option[Identifier] = None,
            templateVariables: TemplateVariablesReader = GlobalTemplateVariables): PluginContext = {
    PluginContext(prefixes, resources, user, projectId, templateVariables)
  }
}
