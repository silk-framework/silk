package org.silkframework.workspace.xml

import java.util.logging.Logger

import org.silkframework.config._
import org.silkframework.rule.{DatasetSelection, TransformRule, TransformSpec}
import org.silkframework.runtime.resource.{ResourceLoader, ResourceManager}
import org.silkframework.runtime.serialization.ReadContext
import org.silkframework.runtime.serialization.XmlSerialization._
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.Identifier
import org.silkframework.util.XMLUtils._

import scala.xml.{Attribute, Null, Text, XML}

/**
 * The transform module, which encapsulates all transform tasks.
 */
private class TransformXmlSerializer extends XmlSerializer[TransformSpec] {

  private val logger = Logger.getLogger(classOf[TransformXmlSerializer].getName)

  override def prefix = "transform"

  /**
   * Writes an updated task.
   */
  override def writeTask(data: Task[TransformSpec], resources: ResourceManager): Unit = {
    val taskResources = resources.child(data.id)

    //Don't use any prefixes
    implicit val prefixes = Prefixes.empty

    taskResources.get("dataset.xml").write() { os => data.selection.toXML(asSource = true).write(os) }
    taskResources.get("rules.xml").writeString(toXml(data).toString())
  }

  /**
   * Loads all tasks of this module.
   */
  override def loadTasks(resources: ResourceLoader, projectResources: ResourceManager): Seq[Task[TransformSpec]] = {
    val tasks =
      for(name <- resources.listChildren) yield
        loadTask(name, resources.child(name), projectResources)
    tasks
  }

  private def loadTask(name: Identifier, taskResources: ResourceLoader, projectResources: ResourceManager) = {
    try {
      implicit val resources = projectResources
      implicit val readContext = ReadContext(resources)
      // Currently the transform spec is distributed in two xml files
      val datasetXml = XML.load(taskResources.get("dataset.xml").load)
      val rulesXml = XML.load(taskResources.get("rules.xml").load)
      var xml = rulesXml.copy(child = datasetXml ++ rulesXml.child)
      // Old XML versions do not contain the id
      if((xml \ "@id").isEmpty) {
        xml = xml % Attribute("id", Text(name), Null)
      }
      fromXml[Task[TransformSpec]](xml)
    } catch {
      case ex: ValidationException =>
        throw new ValidationException(s"Error loading task '$name': ${ex.getMessage}", ex)
    }
  }

  /**
   * Removes a specific task.
   */
  override def removeTask(name: Identifier, resources: ResourceManager): Unit = {
    resources.delete(name.toString)
  }
}