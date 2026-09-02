package org.silkframework.workspace.changes

import org.silkframework.config.Task
import org.silkframework.rule.{ContainerTransformRule, RootMappingRule, RuleTraverser, TransformRule, TransformSpec}
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

  override def describe: String = s"Added mapping rule '${rule.labelOrId}' under '$parentId' in transform '$taskName'"

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
  override def describe: String = {
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
  override def describe: String = s"Updated mapping rule '${after.labelOrId}' in transform '$taskName'"

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

  override def describe: String = s"Reordered mapping rules under '$parentId' in transform '$taskName'"

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
