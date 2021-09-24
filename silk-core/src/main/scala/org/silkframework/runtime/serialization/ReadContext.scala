package org.silkframework.runtime.serialization

import org.silkframework.config.Prefixes
import org.silkframework.runtime.resource.{EmptyResourceManager, ResourceManager}
import org.silkframework.util.IdentifierGenerator

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
                       validationEnabled: Boolean = false)
