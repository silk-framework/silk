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

import org.silkframework.config._
import org.silkframework.dataset._
import org.silkframework.entity.paths.{TypedPath, UntypedPath}
import org.silkframework.entity.{EntitySchema, Restriction, ValueType}
import org.silkframework.execution.typed.{LinkGenerator, LinksEntitySchema}
import org.silkframework.rule.evaluation.ReferenceLinks
import org.silkframework.rule.input.{Input, PathInput, TransformInput, Transformer}
import org.silkframework.rule.similarity.{Aggregation, Comparison, SimilarityOperator}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.plugin.AnyPlugin
import org.silkframework.runtime.plugin.annotations.{Param, Plugin}
import org.silkframework.runtime.plugin.types.IdentifierOptionParameter
import org.silkframework.runtime.resource.Resource
import org.silkframework.runtime.serialization.XmlSerialization._
import org.silkframework.runtime.serialization._
import org.silkframework.runtime.templating.TemplateVariableName
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util._
import org.silkframework.workspace.project.task.DatasetTaskReferenceAutoCompletionProvider
import org.silkframework.workspace.{OriginalTaskData, TaskLoadingException}

import java.util.logging.Logger
import scala.collection.mutable
import scala.xml.Node

/**
 * Represents a Silk Link Specification.
 */
@Plugin(
  id = "linking",
  label = "Linking",
  categories = Array("Linking"),
  description =
      """Generates links between instances from different sources according to a link specification."""
)
case class LinkSpec(@Param(label = "Source input", value = "The source input to select.")
                    source: DatasetSelection = DatasetSelection("SourceDatasetSelection", Uri(""), Restriction.empty),
                    @Param(label = "Target input", value = "The target input to select.")
                    target: DatasetSelection = DatasetSelection("TargetDatasetSelection", Uri(""), Restriction.empty),
                    @Param(label = "Linkage rule", value = "The linkage rule that specifies when entities match and are linked.", visibleInDialog = false)
                    rule: LinkageRule = LinkageRule(),
                    @Param(label = "Output", value = "The output dataset to write the links to.", autoCompletionProvider = classOf[DatasetTaskReferenceAutoCompletionProvider],
                      autoCompleteValueWithLabels = true, allowOnlyAutoCompletedValues = true)
                    output: IdentifierOptionParameter = None,
                    @Param(label = "Reference links", value = "The source input to select.", visibleInDialog = false)
                    referenceLinks: ReferenceLinks = ReferenceLinks.empty,
                    @Param(label = "Link Limit", value = "The maximum number of links that should be generated. The execution will stop once this limit is reached.",
                      advanced = true)
                    linkLimit: Int = LinkSpec.DEFAULT_LINK_LIMIT,
                    @Param(label = "Matching timeout (s)", value = "The timeout in seconds for the matching phase. If the matching takes longer, the execution will be stopped.",
                      advanced = true)
                    matchingExecutionTimeout: Int = LinkSpec.DEFAULT_EXECUTION_TIMEOUT_SECONDS) extends LinkGenerator with AnyPlugin {

  assert(linkLimit >= 0, "The link limit must be greater equal 0!")
  assert(matchingExecutionTimeout >= 0, "The matching execution timeout must be greater equal 0!")

  def dataSelections: DPair[DatasetSelection] = DPair(source, target)

  def findSources(datasets: Iterable[Task[DatasetSpec[Dataset]]])
                 (implicit userContext: UserContext): DPair[DataSource] = {
    DPair.fromSeq(dataSelections.map(_.inputId).map(id => datasets.find(_.id == id).map(_.source).getOrElse(EmptySource)))
  }

  def entityDescriptions: DPair[EntitySchema] = {
    val sourceRestriction = dataSelections.source.restriction
    val targetRestriction = dataSelections.target.restriction

    val sourcePaths = rule.operator match {
      case Some(operator) => collectPaths(true)(operator)
      case None => Set[TypedPath]()
    }

    val targetPaths = rule.operator match {
      case Some(operator) => collectPaths(sourceOrTarget = false)(operator)
      case None => Set[TypedPath]()
    }

    val sourceEntityDesc = EntitySchema(dataSelections.source.typeUri, sourcePaths.toIndexedSeq.distinct, sourceRestriction, UntypedPath.empty)
    val targetEntityDesc = EntitySchema(dataSelections.target.typeUri, targetPaths.toIndexedSeq.distinct, targetRestriction, UntypedPath.empty)

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
      val typedPath = TypedPath(p.path, ValueType.STRING, isAttribute = false)
      Set(typedPath)
    case p: TransformInput => p.inputs.flatMap(collectPathsFromInput).toSet
    case _ => Set()
  }

  /**
    * Accepts exactly two inputs.
    */
  override lazy val inputPorts: InputPorts = {
    FixedNumberOfInputs(entityDescriptions.toSeq.map(FixedSchemaPort))
  }

  /**
    * Output are the generated links.
    */
  override lazy val outputPort: Option[Port] = {
    Some(FixedSchemaPort(LinksEntitySchema.schema))
  }

  override def inputTasks: Set[Identifier] = dataSelections.map(_.inputId).toSet

  override def outputTasks: Set[Identifier] = output.value.toSet

  override lazy val referencedResources: Seq[Resource] = {
    val resources = new mutable.HashSet[Resource]()
    rule.operator foreach (operator => iterateAllTransformersFromSimilarityOperator(operator, _.referencedResources.foreach(resources.add)))
    resources.toSeq
  }

  override def referencedVariables: Seq[TemplateVariableName] = {
    val variables = mutable.Buffer[TemplateVariableName]()
    rule.operator foreach (operator => iterateAllTransformersFromSimilarityOperator(operator, _.referencedVariables.foreach(variables.append)))
    variables.toSeq
  }

  private def iterateAllTransformersFromSimilarityOperator(rule: SimilarityOperator,
                                                           f: Transformer => Unit): Unit = {
    rule match {
      case agg: Aggregation =>
        agg.operators.foreach(op => iterateAllTransformersFromSimilarityOperator(op, f))
      case comp: Comparison =>
        comp.inputs.foreach(input => iterateAllTransformersFromOperator(input, f))
      case _ =>
    }
  }

  private def iterateAllTransformersFromOperator(operator: Operator,
                                                 f: Transformer => Unit): Unit = {
    operator match {
      case TransformInput(_, transformer, inputs) =>
        inputs.foreach(input => iterateAllTransformersFromOperator(input, f))
        f(transformer)
      case _ =>
    }
  }

  override def mainActivities: Seq[String] = Seq("ExecuteLinking")

  override def linkType: Uri = rule.linkType

  override def inverseLinkType: Option[Uri] = rule.inverseLinkType
}

object LinkSpec {
  private val cfg = DefaultConfig.instance()
  private val log: Logger = Logger.getLogger(this.getClass.getCanonicalName)
  val MAX_LINK_LIMIT_CONFIG_KEY = "linking.execution.linkLimit.max"

  val DEFAULT_LINK_LIMIT: Int = {
    cfg.getInt("linking.execution.linkLimit.default")
  }

  /** The absolute maximum of links that can be generated. This is necessary since the links are kept in-memory. */
  val MAX_LINK_LIMIT: Int = {
    cfg.getInt(MAX_LINK_LIMIT_CONFIG_KEY)
  }

  def adaptLinkLimit(requestedLinkLimit: Int): Int = {
    if(requestedLinkLimit > LinkSpec.MAX_LINK_LIMIT) {
      log.warning(s"Link limit of $requestedLinkLimit is higher than the configured max. link limit of $MAX_LINK_LIMIT. " +
          s"Reducing link limit to $MAX_LINK_LIMIT. " +
          s"Increase value of config parameter $MAX_LINK_LIMIT_CONFIG_KEY in order to allow a higher link limit.")
      MAX_LINK_LIMIT
    } else {
      requestedLinkLimit
    }
  }

  val DEFAULT_EXECUTION_TIMEOUT_SECONDS: Int = {
    cfg.getInt("linking.execution.matching.timeout.seconds")
  }

  /**
   * XML serialization format.
   * Reference links are currently not serialized and need to be serialized separately.
   */
  implicit object LinkSpecificationFormat extends XmlFormat[LinkSpec] {

    private val schemaLocation = "org/silkframework/LinkSpecificationLanguage.xsd"

    override def tagNames: Set[String] = Set("Interlink")

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

      // Create link spec
      val linkSpec =
        LinkSpec(
          source = DatasetSelection.fromXML((node \ "SourceDataset").head),
          target = DatasetSelection.fromXML((node \ "TargetDataset").head),
          rule = fromXml[LinkageRule](linkageRuleNode),
          output =
            (node \ "Outputs" \ "Output").headOption map { outputNode =>
              val id = (outputNode \ "@id").text
              if (id.isEmpty) {
                throw new ValidationException(s"Link specification $id contains an output that does not reference a predefined output by id")
              }
              Identifier(id)
            },
          linkLimit = (node \ "@linkLimit").headOption.map(_.text.toInt).getOrElse(LinkSpec.DEFAULT_LINK_LIMIT),
          matchingExecutionTimeout = (node \ "@matchingExecutionTimeout").headOption.map(_.text.toInt).getOrElse(LinkSpec.DEFAULT_EXECUTION_TIMEOUT_SECONDS)
        )

      // Apply templates
      TaskLoadingException.withTaskLoadingException(OriginalTaskData("linking", XmlSerialization.deserializeParameters(node))) { params =>
        linkSpec.withParameters(params)
      }
    }

    /**
     * Serialize a value to XML.
     */
    def write(spec: LinkSpec)(implicit writeContext: WriteContext[Node]): Node =
      <Interlink linkLimit={spec.linkLimit.toString} matchingExecutionTimeout={spec.matchingExecutionTimeout.toString}>
        {spec.dataSelections.source.toXML(asSource = true)}
        {spec.dataSelections.target.toXML(asSource = false)}
        {toXml(spec.rule)}
        <Outputs>
          {spec.output.value.toSeq.map(o => <Output id={o}></Output>)}
        </Outputs>
        {XmlSerialization.serializeParameters(spec.parameters.filterTemplates)}
      </Interlink>
  }
}
