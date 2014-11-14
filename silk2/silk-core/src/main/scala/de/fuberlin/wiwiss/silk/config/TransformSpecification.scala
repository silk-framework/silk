package de.fuberlin.wiwiss.silk.config

import de.fuberlin.wiwiss.silk.dataset.{Dataset, DataSink, DataSource}
import de.fuberlin.wiwiss.silk.linkagerule.{LinkageRule, TransformRule}
import de.fuberlin.wiwiss.silk.runtime.resource.ResourceLoader
import de.fuberlin.wiwiss.silk.util.{Identifier, DPair, ValidationException}

import scala.xml.Node

/**
 * This class contains all the required parameters to execute a transform task.
 *
 * @since 2.6.1
 *
 * @see de.fuberlin.wiwiss.silk.execution.ExecuteTransform
 */
case class TransformSpecification(id: Identifier = Identifier.random, input: DataSource, selection: DatasetSelection, rules: Seq[TransformRule], outputs: Seq[DataSink] = Seq.empty)

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
  def fromXML(node: Node, resourceLoader: ResourceLoader, sources: Set[Dataset])(implicit prefixes: Prefixes): TransformSpecification = {

    // Get the Id.
    val id = (node \ "@id").text

    // Get the required parameters from the XML configuration.
    val datasetSelection = DatasetSelection.fromXML((node \ "SourceDataset").head)
    val dataset = sources.filter(datasetSelection.datasetId == _.id).head
    val dataSource = dataset.source
    val rules = (node \ "TransformRule").map(TransformRule.fromXML(_, resourceLoader))
    val sinks = (node \ "Outputs" \ "Output").map(Dataset.fromXML(_, resourceLoader).sink)

    // Create and return a TransformSpecification instance.
    TransformSpecification(id, dataSource, datasetSelection, rules, sinks)

  }

}