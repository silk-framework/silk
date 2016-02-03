/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.silkframework.config

import java.util.logging.Logger

import org.silkframework.dataset.{LinkSink, DataSink, DataSource, Dataset}
import org.silkframework.entity.{Restriction, EntitySchema, Path}
import org.silkframework.entity.rdf.SparqlEntitySchema
import org.silkframework.evaluation.ReferenceLinks
import org.silkframework.rule.LinkageRule
import org.silkframework.rule.input.{Input, PathInput, TransformInput}
import org.silkframework.rule.similarity.{Aggregation, Comparison, SimilarityOperator}
import org.silkframework.runtime.resource.{ResourceManager, EmptyResourceManager, ResourceLoader}
import org.silkframework.runtime.serialization.Serialization._
import org.silkframework.runtime.serialization.{ValidatingXMLReader, ValidationException, XmlFormat}
import org.silkframework.util._

import scala.xml.Node

/**
 * Represents a Silk Link Specification.
 */
case class LinkSpecification(id: Identifier = Identifier.random,
                             dataSelections: DPair[DatasetSelection] = DatasetSelection.emptyPair,
                             rule: LinkageRule = LinkageRule(),
                             outputs: Seq[Identifier] = Seq.empty,
                             referenceLinks: ReferenceLinks = ReferenceLinks.empty ) {

  def findSources(datasets: Traversable[Dataset]): DPair[DataSource] = {
    DPair.fromSeq(dataSelections.map(_.datasetId).map(id => datasets.find(_.id == id).getOrElse(Dataset.empty).source))
  }

  def findOutputs(datasets: Traversable[Dataset]): Seq[LinkSink] = {
    outputs.flatMap(id => datasets.find(_.id == id)).map(_.linkSink)
  }
  
  def entityDescriptions: DPair[EntitySchema] = {
    val sourceRestriction = dataSelections.source.restriction
    val targetRestriction = dataSelections.target.restriction

    val sourcePaths = rule.operator match {
      case Some(operator) => collectPaths(sourceOrTarget = true)(operator)
      case None => Set[Path]()
    }

    val targetPaths = rule.operator match {
      case Some(operator) => collectPaths(sourceOrTarget = false)(operator)
      case None => Set[Path]()
    }

    val sourceEntityDesc = EntitySchema(dataSelections.source.typeUri, sourcePaths.toIndexedSeq, sourceRestriction)
    val targetEntityDesc = EntitySchema(dataSelections.target.typeUri, targetPaths.toIndexedSeq, targetRestriction)

    DPair(sourceEntityDesc, targetEntityDesc)
  }

  private def collectPaths(sourceOrTarget: Boolean)(operator: SimilarityOperator): Set[Path] = operator match {
    case aggregation: Aggregation => aggregation.operators.flatMap(collectPaths(sourceOrTarget)).toSet
    case comparison: Comparison => {
      if(sourceOrTarget)
        collectPathsFromInput(comparison.inputs.source)
      else
        collectPathsFromInput(comparison.inputs.target)
    }
  }

  private def collectPathsFromInput(param: Input): Set[Path] = param match {
    case p: PathInput if p.path.operators.nonEmpty => Set(p.path)
    case p: TransformInput => p.inputs.flatMap(collectPathsFromInput).toSet
    case _ => Set()
  }
}

object LinkSpecification {

  private val logger = Logger.getLogger(LinkSpecification.getClass.getName)

  /**
   * XML serialization format.
   * Reference links are currently not serialized and need to be serialized separately.
   */
  implicit object LinkSpecificationFormat extends XmlFormat[LinkSpecification] {

    private val schemaLocation = "org/silkframework/LinkSpecificationLanguage.xsd"

    /**
     * Deserialize a value from XML.
     */
    def read(node: Node)(implicit prefixes: Prefixes = Prefixes.empty, resources: ResourceManager = EmptyResourceManager): LinkSpecification = {
      // Validate against XSD Schema
      ValidatingXMLReader.validate(node, schemaLocation)

      //Read id
      val id = (node \ "@id").text

      //Read linkage rule node
      val linkConditionNode = (node \ "LinkCondition").headOption
      val linkageRuleNode = (node \ "LinkageRule").headOption.getOrElse(linkConditionNode.get)

      if (linkageRuleNode.isEmpty && linkConditionNode.isEmpty) throw new ValidationException("No <LinkageRule> found in link specification with id '" + id + "'")
      if (linkConditionNode.isDefined) throw new ValidationException("<LinkCondition> has been renamed to <LinkageRule>. Please update the link specification.")

      LinkSpecification(
        id = id,
        dataSelections = new DPair(DatasetSelection.fromXML((node \ "SourceDataset").head),
          DatasetSelection.fromXML((node \ "TargetDataset").head)),
        rule = fromXml[LinkageRule](linkageRuleNode),
        outputs =
          for(outputNode <- node \ "Outputs" \ "Output") yield {
            val id = (outputNode \ "@id").text
            if(id.isEmpty)
              throw new ValidationException(s"Link specification $id contains an output that does not reference a predefined output by id")
            Identifier(id)
          }
      )
    }

    /**
     * Serialize a value to XML.
     */
    def write(spec: LinkSpecification)(implicit prefixes: Prefixes = Prefixes.empty): Node =
      <Interlink id={spec.id}>
        {spec.dataSelections.source.toXML(asSource = true)}
        {spec.dataSelections.target.toXML(asSource = false)}
        {toXml(spec.rule)}
        <Outputs>
          {spec.outputs.map(o => <Output id={o}></Output>)}
        </Outputs>
      </Interlink>
  }
}
