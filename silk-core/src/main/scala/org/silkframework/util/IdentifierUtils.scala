package org.silkframework.util

import java.util.UUID

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

  def generateIdFromLabel(label: String, defaultPrefix: String): Identifier = {
    val prefix = if (Identifier.fromAllowed(label, alternative = Some(defaultPrefix)) == Identifier(defaultPrefix)) {
      defaultPrefix
    } else {
      label
    }
    // Shortened UUID, not truly unique, but should suffice
    val randomString = UUID.randomUUID().toString
      .grouped(2).map(_.head).mkString
    val inputIdString = s"${prefix}_$randomString".replaceAll("-", "")
    Identifier.fromAllowed(inputIdString)
  }
}
