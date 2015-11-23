package org.silkframework.config

import org.silkframework.entity.rdf.SparqlEntitySchema
import org.silkframework.rule.TransformRule
import org.silkframework.runtime.resource.ResourceLoader
import org.silkframework.runtime.serialization.Serialization._
import org.silkframework.util.Identifier

import scala.xml.Node

/**
 * This class contains all the required parameters to execute a transform task.
 *
 * @since 2.6.1
 *
 * @see org.silkframework.execution.ExecuteTransform
 */
case class TransformSpecification(id: Identifier = Identifier.random, selection: DatasetSelection, rules: Seq[TransformRule], outputs: Seq[Identifier] = Seq.empty) {

  def entityDescription = {
    new SparqlEntitySchema(
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
    val sinks = (node \ "Outputs" \ "Output" \ "@id").map(_.text).map(Identifier(_))

    // Create and return a TransformSpecification instance.
    TransformSpecification(id, datasetSelection, rules, sinks)

  }

}