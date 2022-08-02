package org.silkframework.runtime.serialization

import org.silkframework.config.Prefixes
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.resource.{EmptyResourceManager, ResourceManager}
import org.silkframework.util.{Identifier, IdentifierGenerator}
import org.silkframework.workspace.ProjectTrait

/** Holds context information when deserializing data.
  *
  * @param resources           Resources, e.g. project resources.
  * @param prefixes            (Project) prefixes.
  * @param identifierGenerator ID generator that is assumed to generate unique IDs.
  * @param validationEnabled   If this is set to true then deserializing objects might throw validation exceptions.
  *                            If set to false then no explicit/additional validation is done. Validation done inside the constructors
  *                            of business objects is of course still executed.
  */
case class ReadContext(resources: ResourceManager = EmptyResourceManager(),
                       prefixes: Prefixes = Prefixes.empty,
                       identifierGenerator: IdentifierGenerator = new IdentifierGenerator(),
                       validationEnabled: Boolean = false,
                       user: UserContext = UserContext.Empty,
                       projectId: Option[Identifier] = None) extends PluginContext

object ReadContext {

  def fromProject(project: ProjectTrait)(implicit user: UserContext): ReadContext = {
    ReadContext(
      resources = project.resources,
      prefixes = project.config.prefixes,
      user = user,
      projectId = Some(project.id)
    )
  }

}
