package de.fuberlin.wiwiss.silk.config

import de.fuberlin.wiwiss.silk.dataset.Dataset
import de.fuberlin.wiwiss.silk.entity.EntityDescription
import de.fuberlin.wiwiss.silk.linkagerule.TransformRule
import de.fuberlin.wiwiss.silk.runtime.resource.ResourceLoader
import de.fuberlin.wiwiss.silk.util.Identifier
import de.fuberlin.wiwiss.silk.runtime.serialization.Serialization._
import scala.xml.Node

/**
 * This class contains all the required parameters to execute a transform task.
 *
 * @since 2.6.1
 *
 * @see de.fuberlin.wiwiss.silk.execution.ExecuteTransform
 */
case class TransformSpecification(id: Identifier = Identifier.random, selection: DatasetSelection, rules: Seq[TransformRule], outputs: Seq[Dataset] = Seq.empty) {

  def entityDescription = {
    new EntityDescription(
      variable = selection.variable,
      restrictions = selection.restriction,
      paths = rules.flatMap(_.paths).distinct.toIndexedSeq
    )
  }

}

/**
 * Static functions for the TransformSpecification class.
 */
object TransformSpecification {

  /**
   * Create a TransformSpecification instance using the provided XML node.
   *
   * @since 2.6.1
   *
   * @param node The *Transform* XML node.
   * @param resourceLoader A ResourceLoader instance.
   * @return A TransformSpecification instance.
   */
  def fromXML(node: Node, resourceLoader: ResourceLoader)(implicit prefixes: Prefixes): TransformSpecification = {

    // Get the Id.
    val id = (node \ "@id").text

    // Get the required parameters from the XML configuration.
    val datasetSelection = DatasetSelection.fromXML((node \ "SourceDataset").head)
    val rules = (node \ "TransformRule").map(fromXml[TransformRule])
    val sinks = (node \ "Outputs" \ "Output").map(fromXml[Dataset])

    // Create and return a TransformSpecification instance.
    TransformSpecification(id, datasetSelection, rules, sinks)

  }

}