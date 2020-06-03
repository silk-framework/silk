package controllers.workspaceApi

import java.util.UUID

import org.silkframework.util.Identifier

/**
  * Utility methods for generating valid Identifier.
  */
object IdentifierUtils {
  def generateProjectId(label: String): Identifier = {
    generateIdFromLabel(label, "project")
  }

  def generateTaskId(label: String): Identifier = {
    generateIdFromLabel(label, "task")
  }

  def generateIdFromLabel(label: String, defaultSuffix: String): Identifier = {
    if(Identifier.fromAllowed(label, alternative = Some(defaultSuffix)) == Identifier(defaultSuffix)) {
      Identifier.fromAllowed(UUID.randomUUID().toString + "_" + defaultSuffix)
    } else {
      Identifier.fromAllowed(UUID.randomUUID().toString + "_" + label)
    }
  }
}
