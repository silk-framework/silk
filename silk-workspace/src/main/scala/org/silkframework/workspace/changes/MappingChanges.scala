package org.silkframework.workspace.changes

import org.silkframework.config.Task
import org.silkframework.rule.input.{Input, InputPortInput, PathInput, RuleBlockInput, TransformInput, Transformer}
import org.silkframework.rule.plugins.transformer.value.{ConstantTransformer, ConstantUriTransformer}
import org.silkframework.rule.{ContainerTransformRule, MappingTarget, ObjectMapping, PatternUriMapping, RootMappingRule, RuleTraverser, TransformRule, TransformSpec, TypeMapping, UriMapping, ValueTransformRule}
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.validation.{BadUserInputException, NotFoundException}
import org.silkframework.util.Identifier

/**
  * Adds a mapping rule under a container rule (the root or an object mapping).
  *
  * @param index The position among the parent's children, or None to append. A URI or type rule is stored
  *              ahead of the property rules regardless of the position.
  */
case class AddMapping(taskId: Identifier, parentId: Identifier, rule: TransformRule, index: Option[Int] = None,
                      override val taskLabel: Option[String] = None) extends TaskChange[TransformSpec] {

  override def summary: String = s"Added ${MappingChanges.ruleDisplay(rule)} under '$parentId' in transform '$taskName'"

  // An object rule brings its nested rules along, which the reviewer should see by name.
  override def details: Seq[ChangeDetail] = rule.rules.allRulesRecursive.map(MappingRuleDiff.nestedRule(_, "added"))

  override def inverse: Option[RemoveMapping] = Some(RemoveMapping(taskId, parentId, rule, index, taskLabel))

  override def apply(spec: TransformSpec): TransformSpec = {
    val parent = MappingChanges.container(spec, taskName, parentId)
    for(id <- (rule +: rule.rules.allRulesRecursive).map(_.id) if spec.nestedRuleAndSourcePath(id).isDefined) {
      throw ChangeConflictException(s"A rule with id '$id' already exists in transform '$taskName'.")
    }
    val children = parent.operator.children
    val (before, after) = children.splitAt(index.getOrElse(children.size) min children.size)
    MappingChanges.withRoot(spec, parent.update(parent.operator.withChildren((before :+ rule) ++ after)))
  }
}

object AddMapping {

  /** The addition of a rule under a container rule, as a request names it; each id it brings must be free in the transform. */
  def of(task: Task[TransformSpec], parentId: Identifier, rule: TransformRule, index: Option[Int] = None): AddMapping = {
    val spec = task.data
    MappingChanges.requestedContainer(spec, task.labelOrId, parentId)
    // Names the parent of the rule that holds the id, which the conflict at apply time does not.
    (rule +: rule.rules.allRulesRecursive).foreach(nested => spec.validateNewRuleId(nested.id.toString))
    AddMapping(task.id, parentId, rule, index, Change.capturedName(task))
  }
}

/**
  * Removes a mapping rule. Holds the rule and its position, so the removal can be reverted.
  * Applies only while the rule is unchanged; [[RemoveMapping.of]] captures the current rule.
  */
case class RemoveMapping(taskId: Identifier, parentId: Identifier, rule: TransformRule, index: Option[Int],
                         override val taskLabel: Option[String] = None) extends TaskChange[TransformSpec] {

  // Removing a container rule takes its nested rules with it, which the reviewer should see.
  override def summary: String = {
    val nested = rule.rules.allRulesRecursive.size
    val suffix = if(nested == 1) " and its nested rule" else if(nested > 1) s" and its $nested nested rules" else ""
    s"Removed mapping rule '${rule.labelOrId}'$suffix from transform '$taskName'"
  }

  override def inverse: Option[AddMapping] = Some(AddMapping(taskId, parentId, rule, index, taskLabel))

  override def apply(spec: TransformSpec): TransformSpec = {
    MappingChanges.expectRule(spec, taskName, rule)
    MappingChanges.withRoot(spec, RuleTraverser(spec.mappingRule).remove(rule.id))
  }
}

object RemoveMapping {

  /** The removal of an existing rule of the transform, capturing the rule and its position. */
  def of(task: Task[TransformSpec], ruleId: Identifier): RemoveMapping = {
    val traverser = MappingChanges.requestedRule(task.data, task.labelOrId, ruleId)
    val parent = traverser.moveUp
      .getOrElse(throw BadUserInputException(s"The root rule of transform '${task.labelOrId}' cannot be removed."))
    val index = parent.operator.children.indexWhere(_.id == ruleId)
    RemoveMapping(task.id, parent.operator.id, traverser.operator.asInstanceOf[TransformRule], Some(index),
      Change.capturedName(task))
  }
}

/**
  * Replaces a mapping rule, including its nested rules, by an updated version, which may carry a new id.
  * Applies only while the rule equals `before`; [[UpdateMapping.of]] captures the current rule.
  */
case class UpdateMapping(taskId: Identifier, before: TransformRule, after: TransformRule,
                         override val taskLabel: Option[String] = None) extends TaskChange[TransformSpec] {

  // Names the rule as the update left it, i.e. a rename shows the new label.
  override def summary: String = s"Updated mapping rule '${after.labelOrId}' in transform '$taskName'"

  override def details: Seq[ChangeDetail] = MappingRuleDiff(before, after)

  override def inverse: Option[UpdateMapping] = Some(UpdateMapping(taskId, after, before, taskLabel))

  override def apply(spec: TransformSpec): TransformSpec = {
    val current = MappingChanges.expectRule(spec, taskName, before)
    if(after.id != before.id && spec.nestedRuleAndSourcePath(after.id).isDefined) {
      throw ChangeConflictException(s"A rule with id '${after.id}' already exists in transform '$taskName'.")
    }
    MappingChanges.withRoot(spec, current.update(after))
  }
}

object UpdateMapping {

  /** The update of an existing rule of the transform, capturing the current rule. */
  def of(task: Task[TransformSpec], ruleId: Identifier, updated: TransformRule): UpdateMapping = {
    val current = MappingChanges.requestedRule(task.data, task.labelOrId, ruleId)
    UpdateMapping(task.id, current.operator.asInstanceOf[TransformRule], updated, Change.capturedName(task))
  }
}

/**
  * Reorders the property rules under a container rule; the URI and type rules stay ahead of them.
  * Applies only while the rules are in the `before` order; [[ReorderMappings.of]] captures the current order.
  */
case class ReorderMappings(taskId: Identifier, parentId: Identifier, before: Seq[Identifier], after: Seq[Identifier],
                           override val taskLabel: Option[String] = None) extends TaskChange[TransformSpec] {

  require(before.sorted == after.sorted, "The new order must name each rule of the current order once.")

  override def summary: String = s"Reordered mapping rules under '$parentId' in transform '$taskName'"

  override def inverse: Option[ReorderMappings] = Some(ReorderMappings(taskId, parentId, after, before, taskLabel))

  override def apply(spec: TransformSpec): TransformSpec = {
    val parent = MappingChanges.container(spec, taskName, parentId)
    val rules = parent.operator.asInstanceOf[TransformRule].rules
    if(rules.propertyRules.map(_.id) != before) {
      throw ChangeConflictException(s"The rules under '$parentId' in transform '$taskName' have been changed since.")
    }
    val byId = rules.propertyRules.map(rule => rule.id -> rule).toMap
    val children = rules.uriRule.toSeq ++ rules.typeRules ++ after.map(byId)
    MappingChanges.withRoot(spec, parent.update(parent.operator.withChildren(children)))
  }
}

object ReorderMappings {

  /** The reordering of the property rules under a container rule into `order`, which must name each of them once. */
  def of(task: Task[TransformSpec], parentId: Identifier, order: Seq[String]): ReorderMappings = {
    val current = MappingChanges.requestedContainer(task.data, task.labelOrId, parentId).rules.propertyRules.map(_.id)
    if(order.sorted != current.map(_.toString).sorted) {
      throw BadUserInputException(s"Provided list [${order.mkString(", ")}] does not contain the same elements " +
        s"as current list [${current.mkString(", ")}].")
    }
    ReorderMappings(task.id, parentId, current, order.map(Identifier(_)), Change.capturedName(task))
  }
}

private object MappingChanges {

  /** Names the rule and what it maps for display, e.g. "value mapping 'name' (firstName → http://…/name)". */
  def ruleDisplay(rule: TransformRule): String = {
    s"${kind(rule)} '${rule.labelOrId}'" + essence(rule).map(e => s" ($e)").getOrElse("")
  }

  /** The kind of rule for display, e.g. "value mapping". */
  def kind(rule: TransformRule): String = rule match {
    case _: ObjectMapping => "object mapping"
    case _: TypeMapping => "type mapping"
    case _: UriMapping => "URI mapping"
    case _: ValueTransformRule => "value mapping"
    case _ => "mapping rule"
  }

  /** What the rule maps: its formula (a path as it is) and target property, a type URI or a URI pattern; None if it shows nothing. */
  private def essence(rule: TransformRule): Option[String] = {
    val (source, target) = rule match {
      case typeRule: TypeMapping => (typeRule.typeUri.uri, None)
      case uriRule: PatternUriMapping => (uriRule.pattern, None)
      // The derived operator of an object rule generates its URIs; the object's own source path is the essence.
      case objectRule: ObjectMapping => (objectRule.sourcePath.normalizedSerialization, objectRule.target.map(_.propertyUri.uri))
      case _ => (MappingRuleDiff.formula(rule.operator), rule.target.map(_.propertyUri.uri))
    }
    (Some(source).filter(_.nonEmpty), target) match {
      case (Some(src), Some(tgt)) => Some(s"$src → $tgt")
      case (Some(src), None) => Some(src)
      case (None, Some(tgt)) => Some(s"→ $tgt")
      case (None, None) => None
    }
  }

  /** The rule a request names; that it is missing is the user's error, unlike at apply time ([[rule]]). */
  def requestedRule(spec: TransformSpec, taskName: String, ruleId: Identifier): RuleTraverser = {
    RuleTraverser(spec.mappingRule).find(ruleId)
      .getOrElse(throw new NotFoundException(s"No rule '$ruleId' found in transform '$taskName'."))
  }

  /** The container rule a request names: it must exist and be able to hold child rules. */
  def requestedContainer(spec: TransformSpec, taskName: String, ruleId: Identifier): ContainerTransformRule = {
    requestedRule(spec, taskName, ruleId).operator match {
      case container: ContainerTransformRule => container
      case _ => throw BadUserInputException(s"Rule '$ruleId' in transform '$taskName' cannot hold child rules.")
    }
  }

  /** The rule with the given id, which must be able to hold child rules. */
  def container(spec: TransformSpec, taskName: String, ruleId: Identifier): RuleTraverser = {
    val traverser = rule(spec, taskName, ruleId)
    traverser.operator match {
      case _: ContainerTransformRule => traverser
      case _ => throw ChangeConflictException(s"Rule '$ruleId' in transform '$taskName' cannot hold child rules.")
    }
  }

  /** The rule with the given id. */
  def rule(spec: TransformSpec, taskName: String, ruleId: Identifier): RuleTraverser = {
    RuleTraverser(spec.mappingRule).find(ruleId)
      .getOrElse(throw ChangeConflictException(s"No rule '$ruleId' found in transform '$taskName'."))
  }

  /** The rule with the id of `expected`, which must be unchanged. */
  def expectRule(spec: TransformSpec, taskName: String, expected: TransformRule): RuleTraverser = {
    val traverser = rule(spec, taskName, expected.id)
    if(traverser.operator != expected) {
      throw ChangeConflictException(s"Rule '${expected.id}' in transform '$taskName' has been changed since.")
    }
    traverser
  }

  /** The transform with the tree of the traverser as its root rule. */
  def withRoot(spec: TransformSpec, traverser: RuleTraverser): TransformSpec = {
    spec.copy(mappingRule = traverser.root.operator.asInstanceOf[RootMappingRule])
  }
}

/**
  * What a rule update changed, by the fields of the mapping editor: what the rule maps (value path or formula, target
  * property, data type, URI pattern, type URI), the nested rules of a container by name, then label, description and
  * id. Empty if only something invisible changed, e.g. the operator ids. Also renders the formula and the nested rules
  * for the description of an addition.
  */
private object MappingRuleDiff {

  /** At most this many characters of a formula are shown. */
  private val maxFormulaLength = 200

  def apply(before: TransformRule, after: TransformRule): Seq[ChangeDetail] = {
    val fields = (before, after) match {
      case _ if MappingChanges.kind(before) != MappingChanges.kind(after) =>
        changed("Kind", MappingChanges.kind(before), MappingChanges.kind(after))
      case (b: TypeMapping, a: TypeMapping) => changed("Type", b.typeUri.uri, a.typeUri.uri)
      case (b: PatternUriMapping, a: PatternUriMapping) => changed("URI pattern", b.pattern, a.pattern)
      case (b: UriMapping, a: UriMapping) => changed("URI formula", uriText(b), uriText(a))
      case (b: ContainerTransformRule, a: ContainerTransformRule) =>
        changed("Value path", sourcePath(b), sourcePath(a)) ++ target(b, a) ++ nested(b, a)
      case (b: ValueTransformRule, a: ValueTransformRule) =>
        val value = changed(valueLabel(b, a), formula(b.operator), formula(a.operator))
        val layout = if(value.isEmpty && b.layout != a.layout) Seq(ChangeDetail("Editor layout changed")) else Seq.empty
        value ++ layout ++ target(b, a)
      case _ => Seq.empty
    }
    def text(value: Option[String]): String = VariableChanges.shorten(value.getOrElse(""))
    fields ++ changed("Label", text(before.metaData.label), text(after.metaData.label)) ++
      changed("Description", text(before.metaData.description), text(after.metaData.description)) ++
      changed("Id", before.id.toString, after.id.toString)
  }

  /** A URI rule's pattern, or the formula of a complex one. */
  private def uriText(rule: UriMapping): String = rule match {
    case pattern: PatternUriMapping => pattern.pattern
    case other => formula(other.operator)
  }

  /** The source path of an object rule; the root has none. */
  private def sourcePath(rule: ContainerTransformRule): String = rule match {
    case objectRule: ObjectMapping => objectRule.sourcePath.normalizedSerialization
    case _ => ""
  }

  /** "Value path" while both operators are plain paths, as the editor shows them, else "Value formula". */
  private def valueLabel(before: TransformRule, after: TransformRule): String = (before.operator, after.operator) match {
    case (_: PathInput, _: PathInput) => "Value path"
    case _ => "Value formula"
  }

  /** The target property, data type and flags that differ. */
  private def target(before: TransformRule, after: TransformRule): Seq[ChangeDetail] = {
    def field(label: String, value: MappingTarget => String): Seq[ChangeDetail] = {
      changed(label, before.target.map(value).getOrElse(""), after.target.map(value).getOrElse(""))
    }
    field("Target property", _.propertyUri.uri) ++ field("Data type", _.valueType.label) ++
      field("Single value", _.isAttribute.toString) ++ field("Backward property", _.isBackwardProperty.toString)
  }

  /** The nested rules a container update added, removed or changed, by name; a nested container counts by its own fields. */
  private def nested(before: ContainerTransformRule, after: ContainerTransformRule): Seq[ChangeDetail] = {
    def own(container: ContainerTransformRule): Map[Identifier, TransformRule] = {
      container.rules.allRulesRecursive.map(rule => rule.id -> rule.withChildren(Seq.empty)).toMap
    }
    val (previous, current) = (own(before), own(after))
    after.rules.allRulesRecursive.collect {
      case rule if !previous.contains(rule.id) => nestedRule(rule, "added")
      case rule if previous(rule.id) != rule.withChildren(Seq.empty) => nestedRule(rule, "changed")
    } ++ before.rules.allRulesRecursive.collect {
      case rule if !current.contains(rule.id) => nestedRule(rule, "removed")
    }
  }

  /** A nested rule by name, e.g. "Nested rule 'city' added". */
  def nestedRule(rule: TransformRule, what: String): ChangeDetail = ChangeDetail(s"Nested rule '${rule.labelOrId}' $what")

  private def changed(label: String, previous: String, current: String): Seq[ChangeDetail] = {
    if(previous != current) Seq(ChangeDetail(label, Some(previous), Some(current))) else Seq.empty
  }

  /**
    * An operator tree as a formula, e.g. "lowerCase(trim(name))": a path as it is, a transformer by its plugin id with
    * the parameters that differ from their defaults in brackets, a constant as its quoted value; cut for display.
    */
  def formula(input: Input): String = {
    implicit val context: PluginContext = PluginContext.empty
    VariableChanges.shorten(expression(input), maxFormulaLength)
  }

  private def expression(input: Input)(implicit context: PluginContext): String = input match {
    case PathInput(_, path) => if(path.operators.isEmpty) "(entity)" else path.normalizedSerialization
    case TransformInput(_, constant: ConstantTransformer, _) => quote(constant.value)
    case TransformInput(_, constant: ConstantUriTransformer, _) => s"<${constant.value.uri}>"
    case TransformInput(_, transformer, inputs) =>
      s"${transformer.pluginSpec.id}${parameters(transformer)}(${inputs.map(input => expression(input)).mkString(", ")})"
    case RuleBlockInput(_, ruleBlockId, bindings) =>
      s"$ruleBlockId(${bindings.map(binding => s"${binding.portId} = ${expression(binding.input)}").mkString(", ")})"
    case InputPortInput(_, portId) => s"port:$portId"
  }

  /** The parameters of a transformer that differ from their defaults, e.g. "[regex="\s+", replace=" "]"; empty if none. */
  private def parameters(transformer: Transformer)(implicit context: PluginContext): String = {
    val set = for {
      param <- transformer.pluginSpec.parameters
      value = transformer.templateValues.getOrElse(param.name, param.stringValue(transformer))
      if !param.stringDefaultValue.contains(value)
    } yield s"${param.name}=${quote(value)}"
    if(set.isEmpty) "" else set.mkString("[", ", ", "]")
  }

  private def quote(value: String): String = "\"" + value.replace("\"", "\\\"") + "\""
}
