package org.silkframework.plugins.dataset.hierarchical

import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.Uri

/**
  * Thrown if a written entity references an entity that has not been written to the hierarchical output.
  */
class MissingReferencedEntityException(val referencingEntityUri: String, val referencedEntityUri: String, val property: Uri)
  extends ValidationException(s"Entity <$referencingEntityUri> references entity <$referencedEntityUri> via property <${property.uri}>, " +
    "but no entity with that URI has been written to this dataset. " +
    "A hierarchical output, such as JSON or XML, can only be written if every referenced entity is part of the output.")
