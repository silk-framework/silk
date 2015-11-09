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

package de.fuberlin.wiwiss.silk.config

import java.util.logging.Logger

import de.fuberlin.wiwiss.silk.dataset.{DataSink, DataSource, Dataset}
import de.fuberlin.wiwiss.silk.entity.Path
import de.fuberlin.wiwiss.silk.entity.rdf.SparqlEntitySchema
import de.fuberlin.wiwiss.silk.evaluation.ReferenceLinks
import de.fuberlin.wiwiss.silk.rule.LinkageRule
import de.fuberlin.wiwiss.silk.rule.input.{Input, PathInput, TransformInput}
import de.fuberlin.wiwiss.silk.rule.similarity.{Aggregation, Comparison, SimilarityOperator}
import de.fuberlin.wiwiss.silk.runtime.resource.{ResourceManager, EmptyResourceManager, ResourceLoader}
import de.fuberlin.wiwiss.silk.runtime.serialization.Serialization._
import de.fuberlin.wiwiss.silk.runtime.serialization.{ValidatingXMLReader, ValidationException, XmlFormat}
import de.fuberlin.wiwiss.silk.util._

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

  def findOutputs(datasets: Traversable[Dataset]): Seq[DataSink] = {
    outputs.flatMap(id => datasets.find(_.id == id)).map(_.sink)
  }
  
  def entityDescriptions: DPair[SparqlEntitySchema] = {
    val sourceVar = dataSelections.source.variable
    val targetVar = dataSelections.target.variable

    val sourceRestriction = dataSelections.source.restriction
    val targetRestriction = dataSelections.target.restriction

    val sourcePaths = rule.operator match {
      case Some(operator) => collectPaths(sourceVar)(operator)
      case None => Set[Path]()
    }

    val targetPaths = rule.operator match {
      case Some(operator) => collectPaths(targetVar)(operator)
      case None => Set[Path]()
    }

    val sourceEntityDesc = new SparqlEntitySchema(sourceVar, sourceRestriction, sourcePaths.toIndexedSeq)
    val targetEntityDesc = new SparqlEntitySchema(targetVar, targetRestriction, targetPaths.toIndexedSeq)

    DPair(sourceEntityDesc, targetEntityDesc)
  }

  private def collectPaths(variable: String)(operator: SimilarityOperator): Set[Path] = operator match {
    case aggregation: Aggregation => aggregation.operators.flatMap(collectPaths(variable)).toSet
    case comparison: Comparison => {
      val sourcePaths = collectPathsFromInput(variable)(comparison.inputs.source)
      val targetPaths = collectPathsFromInput(variable)(comparison.inputs.target)
      (sourcePaths ++ targetPaths).toSet
    }
  }

  private def collectPathsFromInput(variable: String)(param: Input): Set[Path] = param match {
    case p: PathInput if p.path.variable == variable && p.path.operators.nonEmpty => Set(p.path)
    case p: TransformInput => p.inputs.flatMap(collectPathsFromInput(variable)).toSet
    case _ => Set()
  }
}

object LinkSpecification {

  private val logger = Logger.getLogger(LinkSpecification.getClass.getName)

  /**
   * XML serialization format.
   * Reference links are currently not serialized and need to be serialize separably.
   */
  implicit object LinkSpecificationFormat extends XmlFormat[LinkSpecification] {

    private val schemaLocation = "de/fuberlin/wiwiss/silk/LinkSpecificationLanguage.xsd"

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
