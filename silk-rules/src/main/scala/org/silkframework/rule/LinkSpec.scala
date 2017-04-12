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

package org.silkframework.rule

import java.util.logging.Logger

import org.silkframework.config.TaskSpec
import org.silkframework.dataset.{DataSource, DatasetTask, LinkSink}
import org.silkframework.entity.{EntitySchema, Path, StringValueType, TypedPath}
import org.silkframework.execution.local.LinksTable
import org.silkframework.rule.evaluation.ReferenceLinks
import org.silkframework.rule.input.{Input, PathInput, TransformInput}
import org.silkframework.rule.similarity.{Aggregation, Comparison, SimilarityOperator}
import org.silkframework.runtime.serialization.XmlSerialization._
import org.silkframework.runtime.serialization.{ReadContext, ValidatingXMLReader, WriteContext, XmlFormat}
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util._

import scala.xml.Node

/**
 * Represents a Silk Link Specification.
 */
case class LinkSpec(dataSelections: DPair[DatasetSelection] = DatasetSelection.emptyPair,
                    rule: LinkageRule = LinkageRule(),
                    outputs: Seq[Identifier] = Seq.empty,
                    referenceLinks: ReferenceLinks = ReferenceLinks.empty ) extends TaskSpec {

  def findSources(datasets: Traversable[DatasetTask]): DPair[DataSource] = {
    DPair.fromSeq(dataSelections.map(_.inputId).map(id => datasets.find(_.id == id).getOrElse(DatasetTask.empty).source))
  }

  def findOutputs(datasets: Traversable[DatasetTask]): Seq[LinkSink] = {
    outputs.flatMap(id => datasets.find(_.id == id)).map(_.linkSink)
  }

  def entityDescriptions: DPair[EntitySchema] = {
    val sourceRestriction = dataSelections.source.restriction
    val targetRestriction = dataSelections.target.restriction

    val sourcePaths = rule.operator match {
      case Some(operator) => collectPaths(sourceOrTarget = true)(operator)
      case None => Set[TypedPath]()
    }

    val targetPaths = rule.operator match {
      case Some(operator) => collectPaths(sourceOrTarget = false)(operator)
      case None => Set[TypedPath]()
    }

    val sourceEntityDesc = EntitySchema(dataSelections.source.typeUri, sourcePaths.toIndexedSeq.distinct, sourceRestriction, Path.empty)
    val targetEntityDesc = EntitySchema(dataSelections.target.typeUri, targetPaths.toIndexedSeq.distinct, targetRestriction, Path.empty)

    DPair(sourceEntityDesc, targetEntityDesc)
  }

  private def collectPaths(sourceOrTarget: Boolean)(operator: SimilarityOperator): Set[TypedPath] = operator match {
    case aggregation: Aggregation => aggregation.operators.flatMap(collectPaths(sourceOrTarget)).toSet
    case comparison: Comparison => {
      if(sourceOrTarget) {
        collectPathsFromInput(comparison.inputs.source)
      } else {
        collectPathsFromInput(comparison.inputs.target)
      }
    }
  }

  private def collectPathsFromInput(param: Input): Set[TypedPath] = param match {
    case p: PathInput if p.path.operators.nonEmpty =>
      // FIXME: LinkSpecs do not support input type definitions, support other types than Strings?
      val typedPath = TypedPath(p.path, StringValueType)
      Set(typedPath)
    case p: TransformInput => p.inputs.flatMap(collectPathsFromInput).toSet
    case _ => Set()
  }

  /**
    * The schemata of the input data for this task.
    * A separate entity schema is returned for each input.
    */
  override lazy val inputSchemataOpt: Option[Seq[EntitySchema]] = {
    Some(entityDescriptions.toSeq)
  }

  /**
    * The schema of the output data.
    * Returns None, if the schema is unknown or if no output is written by this task.
    */
  override lazy val outputSchemaOpt: Option[EntitySchema] = Some(LinksTable.linkEntitySchema)

  override lazy val referencedTasks = dataSelections.map(_.inputId).toSet
}

object LinkSpec {

  private val logger = Logger.getLogger(LinkSpec.getClass.getName)

  /**
   * XML serialization format.
   * Reference links are currently not serialized and need to be serialized separately.
   */
  implicit object LinkSpecificationFormat extends XmlFormat[LinkSpec] {

    private val schemaLocation = "org/silkframework/LinkSpecificationLanguage.xsd"

    /**
     * Deserialize a value from XML.
     */
    def read(node: Node)(implicit readContext: ReadContext): LinkSpec = {
      // Validate against XSD Schema
      ValidatingXMLReader.validate(node, schemaLocation)

      //Read linkage rule node
      val linkConditionNode = (node \ "LinkCondition").headOption
      val linkageRuleNode = (node \ "LinkageRule").headOption.getOrElse(linkConditionNode.get)

      if (linkageRuleNode.isEmpty && linkConditionNode.isEmpty) throw new ValidationException("No <LinkageRule> found in link specification")
      if (linkConditionNode.isDefined) throw new ValidationException("<LinkCondition> has been renamed to <LinkageRule>. Please update the link specification.")

      LinkSpec(
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
    def write(spec: LinkSpec)(implicit writeContext: WriteContext[Node]): Node =
      <Interlink>
        {spec.dataSelections.source.toXML(asSource = true)}
        {spec.dataSelections.target.toXML(asSource = false)}
        {toXml(spec.rule)}
        <Outputs>
          {spec.outputs.map(o => <Output id={o}></Output>)}
        </Outputs>
      </Interlink>
  }
}
