package de.fuberlin.wiwiss.silk.workspace.xml

import java.util.logging.Logger

import de.fuberlin.wiwiss.silk.config.{DatasetSelection, Prefixes, TransformSpecification}
import de.fuberlin.wiwiss.silk.rule.TransformRule
import de.fuberlin.wiwiss.silk.runtime.resource.{ResourceLoader, ResourceManager}
import de.fuberlin.wiwiss.silk.runtime.serialization.Serialization._
import de.fuberlin.wiwiss.silk.util.Identifier
import de.fuberlin.wiwiss.silk.util.XMLUtils._

import scala.xml.XML

/**
 * The transform module, which encapsulates all transform tasks.
 */
private class TransformXmlSerializer extends XmlSerializer[TransformSpecification] {

  private val logger = Logger.getLogger(classOf[TransformXmlSerializer].getName)

  override def prefix = "transform"

  /**
   * Writes an updated task.
   */
  override def writeTask(data: TransformSpecification, resources: ResourceManager): Unit = {
    val taskResources = resources.child(data.id)

    //Don't use any prefixes
    implicit val prefixes = Prefixes.empty

    taskResources.get("dataset.xml").write { os => data.selection.toXML(asSource = true).write(os) }
    taskResources.get("rules.xml").write { os =>
      <TransformSpec>
        { data.rules.map(toXml[TransformRule]) }
        <Outputs>
        { data.outputs.map(o => <Output id={o}></Output>) }
        </Outputs>
      </TransformSpec>.write(os)
    }
  }

  /**
   * Loads all tasks of this module.
   */
  override def loadTasks(resources: ResourceLoader, projectResources: ResourceManager): Map[Identifier, TransformSpecification] = {
    val tasks =
      for(name <- resources.listChildren) yield
        loadTask(name, resources.child(name), projectResources)
    tasks.toMap
  }

  private def loadTask(name: Identifier, taskResources: ResourceLoader, projectResources: ResourceManager) = {
    implicit val resources = projectResources
    val dataset = DatasetSelection.fromXML(XML.load(taskResources.get("dataset.xml").load))
    val rulesXml = XML.load(taskResources.get("rules.xml").load)
    val rules = (rulesXml \ "TransformRule").map(fromXml[TransformRule])
    val outputs = (rulesXml \ "Outputs" \ "Output" \ "@id").map(_.text).map(Identifier(_))
    (name, TransformSpecification(name, dataset, rules, outputs))
  }

  /**
   * Removes a specific task.
   */
  override def removeTask(name: Identifier, resources: ResourceManager): Unit = {
    resources.delete(name)
  }
}