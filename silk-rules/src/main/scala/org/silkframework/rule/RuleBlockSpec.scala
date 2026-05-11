package org.silkframework.rule

import org.silkframework.config.Task.TaskFormat
import org.silkframework.config.{InputPorts, MetaData, Port, Task, TaskSpec}
import org.silkframework.rule.input.{Input, PathInput, TransformInput, Transformer}
import org.silkframework.runtime.plugin.annotations.{Param, Plugin}
import org.silkframework.runtime.plugin.{AnyPlugin, PluginObjectParameterNoSchema}
import org.silkframework.runtime.resource.Resource
import org.silkframework.runtime.serialization.XmlSerialization
import org.silkframework.runtime.serialization.XmlSerialization.{fromXml, toXml}
import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat}
import org.silkframework.runtime.templating.TemplateVariableName
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.Identifier
import org.silkframework.workspace.{OriginalTaskData, TaskLoadingException}
import org.silkframework.workspace.annotation.UiAnnotations

import scala.collection.mutable
import scala.language.implicitConversions
import scala.xml.{Elem, Node, Null, PCData}

@Plugin(
  id = "ruleBlock",
  label = "Rule Block",
  categories = Array("Transform"),
  description = "Defines a reusable transform rule block that can be referenced from transform and linking rules."
)
case class RuleBlockSpec(@Param(label = "", value = "", visibleInDialog = false)
                         content: RuleBlockContent = RuleBlockContent.empty) extends TaskSpec with AnyPlugin {

  validate()

  def ports: IndexedSeq[RuleBlockPort] = content.ports

  def operator: Option[Input] = content.operator

  def layout: RuleLayout = content.layout

  def uiAnnotations: UiAnnotations = content.uiAnnotations

  override def inputPorts: InputPorts = InputPorts.NoInputPorts

  override def outputPort: Option[Port] = None

  override lazy val referencedResources: Seq[Resource] = {
    val resources = mutable.HashSet[Resource]()
    operator.foreach(iterateAllTransformersFromOperator(_, _.referencedResources.foreach(resources.add)))
    resources.toSeq
  }

  override def resourceUpdated(resource: Resource): Unit = {
    operator.foreach(updateResourceOfOperator(_, resource))
  }

  override def referencedVariables: Seq[TemplateVariableName] = {
    val variables = mutable.Buffer[TemplateVariableName]()
    operator.foreach(iterateAllTransformersFromOperator(_, _.referencedVariables.foreach(variables.append)))
    variables.toSeq
  }

  private def validate(): Unit = {
    val duplicatePortIds = ports.map(_.id).groupBy(identity).collect {
      case (id, duplicates) if duplicates.size > 1 => id
    }.toSeq
    if(duplicatePortIds.nonEmpty) {
      throw new ValidationException(s"Duplicate rule block port IDs found: ${duplicatePortIds.mkString(", ")}")
    }
    operator.foreach { op =>
      validateOperatorTree(op)
      op.validateIds()
    }
  }

  private def validateOperatorTree(op: Input): Unit = op match {
    case _: PathInput =>
      throw new ValidationException("Rule blocks must not contain path inputs.")
    case TransformInput(_, _, inputs) =>
      inputs.foreach(validateOperatorTree)
    case other =>
      throw new ValidationException(
        s"Rule blocks currently only support transform operators inside the internal tree, but found '${other.getClass.getSimpleName}'."
      )
  }

  private def iterateAllTransformersFromOperator(operator: Input, f: Transformer => Unit): Unit = {
    operator match {
      case TransformInput(_, transformer, inputs) =>
        inputs.foreach(input => iterateAllTransformersFromOperator(input, f))
        f(transformer)
      case _ =>
    }
  }

  private def updateResourceOfOperator(operator: Input, resource: Resource): Unit = {
    operator match {
      case TransformInput(_, transformer, inputs) =>
        inputs.foreach(input => updateResourceOfOperator(input, resource))
        if(transformer.referencedResources.exists(_.path == resource.path)) {
          transformer.resourceUpdated(resource)
        }
      case _ =>
    }
  }
}

case class RuleBlockTask(id: Identifier,
                         data: RuleBlockSpec,
                         metaData: MetaData = MetaData.empty) extends Task[RuleBlockSpec] {
  override def taskType: Class[_] = classOf[RuleBlockSpec]
}

case class RuleBlockContent(ports: IndexedSeq[RuleBlockPort] = IndexedSeq.empty,
                            operator: Option[Input] = None,
                            layout: RuleLayout = RuleLayout(),
                            uiAnnotations: UiAnnotations = UiAnnotations()) extends PluginObjectParameterNoSchema

object RuleBlockContent {
  val empty: RuleBlockContent = RuleBlockContent()

  implicit object RuleBlockContentXmlFormat extends XmlFormat[RuleBlockContent] {
    override def read(xml: Node)(implicit readContext: ReadContext): RuleBlockContent = {
      val ports = (xml \ "Ports" \ "Port").map(RuleBlockPort.RuleBlockPortXmlFormat.read).toIndexedSeq
      val operator = (xml \ "OperatorTree" \ "_").headOption
        .map(fromXml[Input])
      val layout = (xml \ "RuleLayout").headOption.map(fromXml[RuleLayout]).getOrElse(RuleLayout())
      val uiAnnotations = (xml \ "UiAnnotations").headOption.map(fromXml[UiAnnotations]).getOrElse(UiAnnotations())
      RuleBlockContent(ports, operator, layout, uiAnnotations)
    }

    override def write(value: RuleBlockContent)(implicit writeContext: WriteContext[Node]): Node = {
      <RuleBlockContent>
        <Ports>
          {value.ports.map(RuleBlockPort.RuleBlockPortXmlFormat.write)}
        </Ports>
        <OperatorTree>
          {value.operator match {
          case Some(op) => toXml[Input](op)
          case None => Null
        }}
        </OperatorTree>
        {toXml[RuleLayout](value.layout)}
        {toXml[UiAnnotations](value.uiAnnotations)}
      </RuleBlockContent>
    }
  }
}

case class RuleBlockPort(id: Identifier = Operator.generateId,
                         label: String = "",
                         description: String = "",
                         exampleValues: String = "",
                         displayOrder: Int = 0,
                         deprecated: Boolean = false)

object RuleBlockPort {
  implicit object RuleBlockPortXmlFormat extends XmlFormat[RuleBlockPort] {
    override def read(xml: Node)(implicit readContext: ReadContext): RuleBlockPort = {
      val id = (xml \ "@id").headOption.map(attr => Identifier(attr.text)).getOrElse(Operator.generateId)
      val label = (xml \ "@label").text
      val displayOrder = (xml \ "@displayOrder").headOption.map(_.text.toInt).getOrElse(0)
      val deprecated = (xml \ "@deprecated").headOption.map(_.text.toBoolean).getOrElse(false)
      val description = (xml \ "Description").text
      val exampleValues = (xml \ "ExampleValues").text
      RuleBlockPort(id, label, description, exampleValues, displayOrder, deprecated)
    }

    override def write(value: RuleBlockPort)(implicit writeContext: WriteContext[Node]): Node = {
      <Port id={value.id.toString}
            label={value.label}
            displayOrder={value.displayOrder.toString}
            deprecated={value.deprecated.toString}>
        <Description xml:space="preserve">{PCData(value.description)}</Description>
        <ExampleValues xml:space="preserve">{PCData(value.exampleValues)}</ExampleValues>
      </Port>
    }
  }
}

object RuleBlockSpec {
  implicit def toRuleBlockTask(task: Task[RuleBlockSpec]): RuleBlockTask = {
    RuleBlockTask(task.id, task.data, task.metaData)
  }

  def empty: RuleBlockSpec = RuleBlockSpec()

  implicit object RuleBlockSpecXmlFormat extends XmlFormat[RuleBlockSpec] {
    override def tagNames: Set[String] = Set("RuleBlock")

    override def read(node: Node)(implicit readContext: ReadContext): RuleBlockSpec = {
      val contentNode = (node \ "RuleBlockContent").headOption.getOrElse(node)
      val ruleBlockSpec = RuleBlockSpec(RuleBlockContent.RuleBlockContentXmlFormat.read(contentNode))

      TaskLoadingException.withTaskLoadingException(OriginalTaskData("ruleBlock", XmlSerialization.deserializeParameters(node))) { params =>
        ruleBlockSpec.withParameters(params)
      }
    }

    override def write(value: RuleBlockSpec)(implicit writeContext: WriteContext[Node]): Node = {
      <RuleBlock>
        {RuleBlockContent.RuleBlockContentXmlFormat.write(value.content).child}
        {XmlSerialization.serializeParameters(value.parameters.filterTemplates)}
      </RuleBlock>
    }
  }

  implicit object RuleBlockTaskXmlFormat extends XmlFormat[RuleBlockTask] {
    override def read(value: Node)(implicit readContext: ReadContext): RuleBlockTask = {
      val task = new TaskFormat[RuleBlockSpec].read(value)
      RuleBlockTask(task.id, task.data, task.metaData)
    }

    override def write(value: RuleBlockTask)(implicit writeContext: WriteContext[Node]): Node = {
      new TaskFormat[RuleBlockSpec].write(value)
    }
  }
}
