package org.silkframework.plugins.transformer.value

import java.time.Instant
import java.util.Properties
import java.util.logging.Logger

import org.silkframework.rule.input.Transformer
import org.silkframework.runtime.plugin.annotations.{Param, Plugin}
import org.silkframework.runtime.resource.Resource
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.workspace.resources.ResourceAutoCompletionProvider

@Plugin(
  id = "readParameter",
  label = "Read parameter",
  categories = Array("Value"),
  description = "Reads a parameter from a Java Properties file."
)
case class ReadParameter(
  @Param(value = "The Java properties file to read the parameter from.", autoCompletionProvider = classOf[ResourceAutoCompletionProvider], allowOnlyAutoCompletedValues = true)
  resource: Resource,
  @Param("The name of the parameter.")
  parameter: String) extends Transformer {

  private val logger = Logger.getLogger(classOf[ReadParameter].getName)

  /**
    * The timestamp when the current properties have been loaded.
    */
  private var lastUpdateTime = Instant.MIN

  /**
    * If the resource does not provide a modification time, the properties are reloaded after that many seconds.
    */
  private val UPDATE_INTERVAL = 10

  /**
    * The current value as loaded from the properties file.
    */
  private var value: Option[String] = None

  // Update initially
  update()

  override def apply(values: Seq[Seq[String]]): Seq[String] = {
    updateIfNeeded()
    value.toSeq
  }

  private def updateIfNeeded(): Unit = {
    resource.modificationTime match {
      case Some(modificationTime) if lastUpdateTime isBefore modificationTime =>
        update()
      case None if lastUpdateTime isBefore Instant.now.minusSeconds(UPDATE_INTERVAL) =>
        update()
      case _ =>
        // No need to update
    }
  }

  private def update(): Unit = {
    val properties = new Properties()
    resource.read(properties.load)
    value = Option(properties.getProperty(parameter))
    if(value.isEmpty) {
      throw new ValidationException(s"Resource ${resource.name} does not provide a parameter '$parameter'")
    }
    lastUpdateTime = Instant.now
    logger.fine(s"Updated parameter $parameter from resource ${resource.name}.")
  }

}
