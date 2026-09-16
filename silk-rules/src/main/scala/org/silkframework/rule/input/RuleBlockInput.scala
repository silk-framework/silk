package org.silkframework.rule.input

import org.silkframework.config.Task
import org.silkframework.entity.Entity
import org.silkframework.rule.{Operator, RuleBlockSpec, TaskContext}
import org.silkframework.runtime.serialization.XmlSerialization.{fromXml, toXml}
import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat}
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.Identifier

import scala.xml.Node

/**
 * A binding from a logical rule block port to an outer input subtree.
 */
case class RuleBlockBinding(portId: Identifier,
                            input: Input) {

  private def validate(): Unit = {
    validateInput(input)
  }

  private def validateInput(boundInput: Input): Unit = {
    boundInput match {
      // Names both causes: inside a definition the real violation is the nesting, detected only later.
      case _: InputPortInput =>
        throw new ValidationException("Input ports may only be used inside rule block definitions. " +
          s"The binding of port '$portId' is fed by one; if this rule block usage is itself part of a rule block " +
          "definition, then the actual problem is that nested rule block usages are not supported.")
      case _: RuleBlockInput =>
        throw new ValidationException("Nested rule block usages are not supported in the first iteration. " +
          s"The binding of port '$portId' uses another rule block.")
      case TransformInput(_, _, inputs) =>
        inputs.foreach(validateInput)
      case _: PathInput =>
    }
  }

  validate()
}

object RuleBlockBinding {

  implicit object RuleBlockBindingFormat extends XmlFormat[RuleBlockBinding] {
    override def read(node: Node)(implicit readContext: ReadContext): RuleBlockBinding = {
      val portId = (node \ "@portId").headOption.map(_.text).filter(_.nonEmpty).map(Identifier.apply).getOrElse {
        throw new ValidationException("Rule block binding requires a non-empty 'portId' attribute.")
      }
      val inputNode = (node \ "_").find(child => Input.InputFormat.tagNames.contains(child.label)).getOrElse {
        throw new ValidationException(s"Rule block binding for port '$portId' does not contain a valid input.")
      }
      RuleBlockBinding(portId, fromXml[Input](inputNode))
    }

    override def write(value: RuleBlockBinding)(implicit writeContext: WriteContext[Node]): Node = {
      <Binding portId={value.portId.toString}>
        {toXml[Input](value.input)}
      </Binding>
    }
  }
}

/**
 * An input that references a reusable rule block task.
 */
case class RuleBlockInput(id: Identifier = Operator.generateId,
                          ruleBlockId: Identifier,
                          bindings: IndexedSeq[RuleBlockBinding] = IndexedSeq.empty) extends Input {

  private def validate(): Unit = {
    val duplicateBindingPortIds = bindings.map(_.portId).groupBy(identity).collect {
      case (portId, duplicates) if duplicates.size > 1 => portId
    }.toSeq
    if(duplicateBindingPortIds.nonEmpty) {
      throw new ValidationException(s"Duplicate rule block binding port IDs found: ${duplicateBindingPortIds.mkString(", ")}")
    }
  }

  validate()

  override def children: Seq[Input] = bindings.map(_.input)

  override def withId(newId: Identifier): Operator = copy(id = newId)

  override def withChildren(newChildren: Seq[Operator]): RuleBlockInput = {
    if(newChildren.size != bindings.size) {
      throw new IllegalArgumentException(s"Expected ${bindings.size} children, but got ${newChildren.size}.")
    }
    val updatedBindings = bindings.zip(newChildren).map {
      case (binding, input: Input) =>
        binding.copy(input = input)
      case (_, other) =>
        throw new IllegalArgumentException(s"RuleBlockInput only accepts Input children, but got ${other.getClass.getSimpleName}.")
    }.toIndexedSeq
    copy(bindings = updatedBindings)
  }

  override def execution(taskContext: TaskContext): InputExecution = {
    val ruleBlockTask = taskContext.pluginContext.taskResolver.resolveTyped[RuleBlockSpec](ruleBlockId)
    val ruleBlockPortIds = ruleBlockTask.data.ports.map(_.id).toSet
    val unknownBindingPortIds = bindings.map(_.portId).filterNot(ruleBlockPortIds).distinct
    if(unknownBindingPortIds.nonEmpty) {
      throw new ValidationException(s"Rule block usage '$id' binds ports that rule block '$ruleBlockId' " +
        s"does not define: ${unknownBindingPortIds.mkString(", ")}.")
    }

    val bindingExecutions = bindings.map(binding => RuleBlockBindingExecution(binding.portId, binding.input.execution(taskContext)))
    val ruleBlockExecution = RuleBlockExecution(ruleBlockTask, bindingExecutions, taskContext)
    new RuleBlockInputExecution(this, ruleBlockExecution)
  }
}

/**
 * Runtime form of a [[RuleBlockBinding]].
 */
case class RuleBlockBindingExecution(portId: Identifier,
                                     inputExecution: InputExecution)

/**
 * Runtime form of a resolved rule block definition.
 */
final class RuleBlockExecution(val task: Task[RuleBlockSpec],
                               val bindingExecutions: IndexedSeq[RuleBlockBindingExecution],
                               val rootExecution: Option[InputExecution])

object RuleBlockExecution {
  def apply(task: Task[RuleBlockSpec],
            bindingExecutions: IndexedSeq[RuleBlockBindingExecution],
            taskContext: TaskContext): RuleBlockExecution = {
    val bindingExecutionsByPortId = bindingExecutions.iterator.map(binding => binding.portId -> binding.inputExecution).toMap

    def buildExecution(input: Input): InputExecution = {
      input match {
        case transform: TransformInput =>
          new TransformInputExecution(
            operator = transform,
            inputExecutions = transform.inputs.map(buildExecution),
            transformerExecution = transform.transformer.execution(taskContext)
          )
        case inputPort: InputPortInput =>
          new InputPortExecution(inputPort, bindingExecutionsByPortId.get(inputPort.portId))
        case _: PathInput =>
          throw new ValidationException("Rule blocks must not contain path inputs.")
        case _: RuleBlockInput =>
          throw new ValidationException("Nested rule block usages are not supported in the first iteration.")
      }
    }

    new RuleBlockExecution(task, bindingExecutions, task.data.operator.map(buildExecution))
  }
}

/**
 * Runtime executor for a [[RuleBlockInput]].
 */
final class RuleBlockInputExecution(override val operator: RuleBlockInput,
                                    val ruleBlockExecution: RuleBlockExecution) extends InputExecution {

  override def apply(entity: Entity): Value = {
    ruleBlockExecution.rootExecution.map(_(entity)).getOrElse(Value(Seq.empty))
  }
}

object RuleBlockInput {

  implicit object RuleBlockInputFormat extends XmlFormat[RuleBlockInput] {
    override def tagNames: Set[String] = Set("RuleBlockInput")

    override def read(node: Node)(implicit readContext: ReadContext): RuleBlockInput = {
      val id = Operator.readId(node)
      val ruleBlockId = (node \ "@ruleBlockId").headOption.map(_.text).filter(_.nonEmpty).map(Identifier.apply).getOrElse {
        throw new ValidationException("RuleBlockInput requires a non-empty 'ruleBlockId' attribute.", id, "RuleBlockInput")
      }
      val bindings = (node \ "Binding").map(fromXml[RuleBlockBinding]).toIndexedSeq
      RuleBlockInput(id, ruleBlockId, bindings)
    }

    override def write(value: RuleBlockInput)(implicit writeContext: WriteContext[Node]): Node = {
      <RuleBlockInput id={value.id.toString} ruleBlockId={value.ruleBlockId.toString}>
        {value.bindings.map(toXml[RuleBlockBinding])}
      </RuleBlockInput>
    }
  }
}
