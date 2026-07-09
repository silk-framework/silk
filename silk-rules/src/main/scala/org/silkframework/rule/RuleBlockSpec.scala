package org.silkframework.rule

import org.silkframework.config.Task.TaskFormat
import org.silkframework.config.{InputPorts, MetaData, Port, Task, TaskSpec}
import org.silkframework.rule.input.{Input, InputPortInput, PathInput, TransformInput, Transformer}
import org.silkframework.runtime.plugin.annotations.{Param, Plugin}
import org.silkframework.runtime.plugin.{AnyPlugin, PluginObjectParameterNoSchema}
import org.silkframework.runtime.resource.Resource
import org.silkframework.runtime.serialization.XmlSerialization
import org.silkframework.runtime.serialization.XmlSerialization.{fromXml, toXml}
import org.silkframework.runtime.serialization.{ReadContext, ValidatingXMLReader, WriteContext, XmlFormat}
import org.silkframework.runtime.templating.{TemplateVariableName, TemplateVariables}
import org.silkframework.runtime.validation.{TaskValidationException, ValidationException}
import org.silkframework.util.Identifier
import org.silkframework.workspace.{OriginalTaskData, TaskLoadingException}
import org.silkframework.workspace.annotation.UiAnnotations

import scala.collection.mutable
import scala.language.implicitConversions
import scala.xml.{Elem, Node, Null, PCData}

@Plugin(
  id = "ruleBlock",
  label = "Rule block",
  categories = Array("Transform"),
  description = "Defines a reusable transform rule block that can be referenced from transform and linking rules."
)
case class RuleBlockSpec(@Param(label = "Rule block model",
                                value = "The complete rule block model, i.e. rule tree, port definitions, layout and UI annotations.",
                                visibleInDialog = false)
                         ruleBlockModel: RuleBlockModel = RuleBlockModel.empty) extends TaskSpec with AnyPlugin {

  validate()

  def ports: IndexedSeq[RuleBlockPort] = ruleBlockModel.ports

  def inputExamples: IndexedSeq[RuleBlockInputExample] = ruleBlockModel.inputExamples

  def operator: Option[Input] = ruleBlockModel.operator

  def layout: RuleLayout = ruleBlockModel.layout

  def uiAnnotations: UiAnnotations = ruleBlockModel.uiAnnotations

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
    val definedPortIds = ports.map(_.id).toSet
    val duplicatePortIds = ports.map(_.id).groupBy(identity).collect {
      case (id, duplicates) if duplicates.size > 1 => id
    }.toSeq
    val duplicateInputExampleIds = inputExamples.map(_.id).groupBy(identity).collect {
      case (id, duplicates) if duplicates.size > 1 => id
    }.toSeq
    val duplicateDisplayOrders = ports.map(_.displayOrder).groupBy(identity).collect {
      case (displayOrder, duplicates) if duplicates.size > 1 => displayOrder
    }.toSeq.sorted
    if(duplicatePortIds.nonEmpty) {
      throw new ValidationException(s"Duplicate rule block port IDs found: ${duplicatePortIds.mkString(", ")}")
    }
    if(duplicateDisplayOrders.nonEmpty) {
      throw new ValidationException(s"Duplicate rule block port display orders found: ${duplicateDisplayOrders.mkString(", ")}")
    }
    if(duplicateInputExampleIds.nonEmpty) {
      throw new ValidationException(s"Duplicate rule block input example IDs found: ${duplicateInputExampleIds.mkString(", ")}")
    }
    inputExamples.foreach { example =>
      example.inputs.keys.foreach { portId =>
        if(!definedPortIds.contains(portId)) {
          throw new ValidationException(
            s"Rule block input example '${example.id}' references unknown input port '${portId}'."
          )
        }
      }
    }
    operator.foreach { op =>
      validateOperatorTree(op, definedPortIds)
      op.validateIds()
    }
  }

  private def validateOperatorTree(op: Input, definedPortIds: Set[Identifier]): Unit = op match {
    case _: PathInput =>
      throw new ValidationException("Rule blocks must not contain path inputs.")
    case inputPort: InputPortInput =>
      if(!definedPortIds.contains(inputPort.portId)) {
        throw new ValidationException(s"Rule block input port reference '${inputPort.portId}' does not exist in the rule block port definitions.")
      }
    case TransformInput(_, _, inputs) =>
      inputs.foreach(validateOperatorTree(_, definedPortIds))
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
                         metaData: MetaData = MetaData.empty,
                         executionVariables: TemplateVariables = TemplateVariables.empty) extends Task[RuleBlockSpec] {
  override def taskType: Class[_] = classOf[RuleBlockSpec]
}

case class RuleBlockModel(ports: IndexedSeq[RuleBlockPort] = IndexedSeq.empty,
                          inputExamples: IndexedSeq[RuleBlockInputExample] = IndexedSeq.empty,
                          operator: Option[Input] = None,
                          layout: RuleLayout = RuleLayout(),
                          uiAnnotations: UiAnnotations = UiAnnotations()) extends PluginObjectParameterNoSchema

object RuleBlockModel {
  val empty: RuleBlockModel = RuleBlockModel()

  implicit object RuleBlockModelXmlFormat extends XmlFormat[RuleBlockModel] {
    override def read(xml: Node)(implicit readContext: ReadContext): RuleBlockModel = {
      val ports = (xml \ "Ports" \ "Port").map(RuleBlockPort.RuleBlockPortXmlFormat.read).toIndexedSeq
      val inputExamples = (xml \ "InputExamples" \ "Example").map(RuleBlockInputExample.RuleBlockInputExampleXmlFormat.read).toIndexedSeq
      val operator = (xml \ "OperatorTree" \ "_").headOption
        .map(fromXml[Input])
      val layout = (xml \ "RuleLayout").headOption.map(fromXml[RuleLayout]).getOrElse(RuleLayout())
      val uiAnnotations = (xml \ "UiAnnotations").headOption.map(fromXml[UiAnnotations]).getOrElse(UiAnnotations())
      RuleBlockModel(ports, inputExamples, operator, layout, uiAnnotations)
    }

    override def write(value: RuleBlockModel)(implicit writeContext: WriteContext[Node]): Node = {
      <RuleBlockModel>
        <Ports>
          {value.ports.map(RuleBlockPort.RuleBlockPortXmlFormat.write)}
        </Ports>
        <InputExamples>
          {value.inputExamples.map(RuleBlockInputExample.RuleBlockInputExampleXmlFormat.write)}
        </InputExamples>
        <OperatorTree>
          {value.operator match {
          case Some(op) => toXml[Input](op)
          case None => Null
        }}
        </OperatorTree>
        {toXml[RuleLayout](value.layout)}
        {toXml[UiAnnotations](value.uiAnnotations)}
      </RuleBlockModel>
    }
  }
}

case class RuleBlockPort(id: Identifier = Operator.generateId,
                         label: String,
                         description: String = "",
                         displayOrder: Int,
                         deprecated: Boolean = false) {
  if(label.isEmpty) {
    throw new TaskValidationException("Label of rule block port must not be empty!")
  }
}

case class RuleBlockInputExample(id: Identifier = Operator.generateId,
                                 label: Option[String] = None,
                                 inputs: Map[Identifier, Seq[String]] = Map.empty)

object RuleBlockInputExample {
  implicit object RuleBlockInputExampleXmlFormat extends XmlFormat[RuleBlockInputExample] {
    override def read(xml: Node)(implicit readContext: ReadContext): RuleBlockInputExample = {
      val id = (xml \ "@id").headOption.map(attr => Identifier(attr.text)).getOrElse(Operator.generateId)
      val label = (xml \ "@label").headOption.map(_.text).filter(_.nonEmpty)
      val inputs = (xml \ "Input").map { inputNode =>
        val portId = (inputNode \ "@portId").headOption
          .map(attr => Identifier(attr.text))
          .getOrElse(throw new ValidationException(s"Rule block input example '${id}' contains an input without a portId attribute."))
        val values = (inputNode \ "Value").map(_.text).toSeq
        portId -> values
      }.toMap
      RuleBlockInputExample(id, label, inputs)
    }

    override def write(value: RuleBlockInputExample)(implicit writeContext: WriteContext[Node]): Node = {
      <Example id={value.id.toString} label={value.label.filter(_.nonEmpty).orNull}>
        {value.inputs.toSeq.sortBy(_._1.toString).map { case (portId, values) =>
        <Input portId={portId.toString}>
          {values.map(v => <Value xml:space="preserve">{PCData(v)}</Value>)}
        </Input>
      }}
      </Example>
    }
  }
}

object RuleBlockPort {
  implicit object RuleBlockPortXmlFormat extends XmlFormat[RuleBlockPort] {
    override def read(xml: Node)(implicit readContext: ReadContext): RuleBlockPort = {
      val id = (xml \ "@id").headOption.map(attr => Identifier(attr.text)).getOrElse(Operator.generateId)
      val label = (xml \ "@label").headOption.map(_.text).filter(_.nonEmpty).getOrElse {
        throw new TaskValidationException("Rule block port requires a non-empty 'label' attribute.")
      }
      val displayOrder = (xml \ "@displayOrder").headOption.map(_.text.toInt).getOrElse {
        throw new TaskValidationException(s"Rule block port '$id' requires a 'displayOrder' attribute.")
      }
      val deprecated = (xml \ "@deprecated").headOption.map(_.text.toBoolean).getOrElse(false)
      val description = (xml \ "Description").text
      RuleBlockPort(id, label, description, displayOrder, deprecated)
    }

    override def write(value: RuleBlockPort)(implicit writeContext: WriteContext[Node]): Node = {
      <Port id={value.id.toString}
            label={value.label}
            displayOrder={value.displayOrder.toString}
            deprecated={value.deprecated.toString}>
        <Description xml:space="preserve">{PCData(value.description)}</Description>
      </Port>
    }
  }
}

object RuleBlockSpec {
  implicit def toRuleBlockTask(task: Task[RuleBlockSpec]): RuleBlockTask = {
    RuleBlockTask(task.id, task.data, task.metaData, task.executionVariables)
  }

  def empty: RuleBlockSpec = RuleBlockSpec()

  implicit object RuleBlockSpecXmlFormat extends XmlFormat[RuleBlockSpec] {
    override def tagNames: Set[String] = Set("RuleBlock")

    override def read(node: Node)(implicit readContext: ReadContext): RuleBlockSpec = {
      ValidatingXMLReader.validate(node, "org/silkframework/LinkSpecificationLanguage.xsd")
      val modelNode = (node \ "RuleBlockModel").headOption.getOrElse(node)
      val ruleBlockSpec = RuleBlockSpec(RuleBlockModel.RuleBlockModelXmlFormat.read(modelNode))

      TaskLoadingException.withTaskLoadingException(OriginalTaskData("ruleBlock", XmlSerialization.deserializeParameters(node))) { params =>
        ruleBlockSpec.withParameters(params)
      }
    }

    override def write(value: RuleBlockSpec)(implicit writeContext: WriteContext[Node]): Node = {
      <RuleBlock>
        {RuleBlockModel.RuleBlockModelXmlFormat.write(value.ruleBlockModel).child}
        {XmlSerialization.serializeParameters(value.parameters.filterTemplates)}
      </RuleBlock>
    }
  }

  implicit object RuleBlockTaskXmlFormat extends XmlFormat[RuleBlockTask] {
    override def read(value: Node)(implicit readContext: ReadContext): RuleBlockTask = {
      val task = new TaskFormat[RuleBlockSpec].read(value)
      RuleBlockTask(task.id, task.data, task.metaData, task.executionVariables)
    }

    override def write(value: RuleBlockTask)(implicit writeContext: WriteContext[Node]): Node = {
      new TaskFormat[RuleBlockSpec].write(value)
    }
  }
}
