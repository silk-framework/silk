package org.silkframework.workspace.changes

import org.silkframework.rule.{ContainerTransformRule, RootMappingRule, RuleTraverser, TransformRule, TransformSpec}
import org.silkframework.runtime.validation.{BadUserInputException, NotFoundException}
import org.silkframework.util.Identifier

/**
  * Adds a mapping rule under a container rule (the root or an object mapping).
  *
  * @param index The position among the parent's children, or None to append. A URI or type rule is stored
  *              ahead of the property rules regardless of the position.
  */
case class AddMapping(taskId: Identifier, parentId: Identifier, rule: TransformRule, index: Option[Int] = None)
  extends TaskChange[TransformSpec] {

  override def describe: String = s"Added mapping rule '${rule.id}' under '$parentId' in transform '$taskId'"

  override def inverse: Option[RemoveMapping] = Some(RemoveMapping(taskId, parentId, rule, index))

  override def apply(spec: TransformSpec): TransformSpec = {
    val parent = MappingChanges.container(spec, taskId, parentId)
    for(id <- (rule +: rule.rules.allRulesRecursive).map(_.id) if spec.nestedRuleAndSourcePath(id).isDefined) {
      throw ChangeConflictException(s"A rule with id '$id' already exists in transform '$taskId'.")
    }
    val children = parent.operator.children
    val (before, after) = children.splitAt(index.getOrElse(children.size) min children.size)
    MappingChanges.withRoot(spec, parent.update(parent.operator.withChildren((before :+ rule) ++ after)))
  }
}

/**
  * Removes a mapping rule. Holds the rule and its position, so the removal can be reverted.
  * Applies only while the rule is unchanged; [[RemoveMapping.of]] captures the current rule.
  */
case class RemoveMapping(taskId: Identifier, parentId: Identifier, rule: TransformRule, index: Option[Int])
  extends TaskChange[TransformSpec] {

  override def describe: String = s"Removed mapping rule '${rule.id}' from transform '$taskId'"

  override def inverse: Option[AddMapping] = Some(AddMapping(taskId, parentId, rule, index))

  override def apply(spec: TransformSpec): TransformSpec = {
    MappingChanges.expectRule(spec, taskId, rule)
    MappingChanges.withRoot(spec, RuleTraverser(spec.mappingRule).remove(rule.id))
  }
}

object RemoveMapping {

  /** The removal of an existing rule of the transform, capturing the rule and its position. */
  def of(taskId: Identifier, spec: TransformSpec, ruleId: Identifier): RemoveMapping = {
    val traverser = RuleTraverser(spec.mappingRule).find(ruleId)
      .getOrElse(throw new NotFoundException(s"No rule '$ruleId' found in transform '$taskId'."))
    val parent = traverser.moveUp
      .getOrElse(throw BadUserInputException(s"The root rule of transform '$taskId' cannot be removed."))
    val index = parent.operator.children.indexWhere(_.id == ruleId)
    RemoveMapping(taskId, parent.operator.id, traverser.operator.asInstanceOf[TransformRule], Some(index))
  }
}

/**
  * Replaces a mapping rule, including its nested rules, by an updated version, which may carry a new id.
  * Applies only while the rule equals `before`; [[UpdateMapping.of]] captures the current rule.
  */
case class UpdateMapping(taskId: Identifier, before: TransformRule, after: TransformRule) extends TaskChange[TransformSpec] {

  override def describe: String = s"Updated mapping rule '${before.id}' in transform '$taskId'"

  override def inverse: Option[UpdateMapping] = Some(UpdateMapping(taskId, after, before))

  override def apply(spec: TransformSpec): TransformSpec = {
    val current = MappingChanges.expectRule(spec, taskId, before)
    if(after.id != before.id && spec.nestedRuleAndSourcePath(after.id).isDefined) {
      throw ChangeConflictException(s"A rule with id '${after.id}' already exists in transform '$taskId'.")
    }
    MappingChanges.withRoot(spec, current.update(after))
  }
}

object UpdateMapping {

  /** The update of an existing rule of the transform, capturing the current rule. */
  def of(taskId: Identifier, spec: TransformSpec, ruleId: Identifier, updated: TransformRule): UpdateMapping = {
    val current = RuleTraverser(spec.mappingRule).find(ruleId)
      .getOrElse(throw new NotFoundException(s"No rule '$ruleId' found in transform '$taskId'."))
    UpdateMapping(taskId, current.operator.asInstanceOf[TransformRule], updated)
  }
}

/**
  * Reorders the property rules under a container rule; the URI and type rules stay ahead of them.
  * Applies only while the rules are in the `before` order; [[ReorderMappings.of]] captures the current order.
  */
case class ReorderMappings(taskId: Identifier, parentId: Identifier, before: Seq[Identifier], after: Seq[Identifier])
  extends TaskChange[TransformSpec] {

  require(before.sorted == after.sorted, "The new order must name each rule of the current order once.")

  override def describe: String = s"Reordered mapping rules under '$parentId' in transform '$taskId'"

  override def inverse: Option[ReorderMappings] = Some(ReorderMappings(taskId, parentId, after, before))

  override def apply(spec: TransformSpec): TransformSpec = {
    val parent = MappingChanges.container(spec, taskId, parentId)
    val rules = parent.operator.asInstanceOf[TransformRule].rules
    if(rules.propertyRules.map(_.id) != before) {
      throw ChangeConflictException(s"The rules under '$parentId' in transform '$taskId' have been changed since.")
    }
    val byId = rules.propertyRules.map(rule => rule.id -> rule).toMap
    val children = rules.uriRule.toSeq ++ rules.typeRules ++ after.map(byId)
    MappingChanges.withRoot(spec, parent.update(parent.operator.withChildren(children)))
  }
}

object ReorderMappings {

  /** The reordering of the property rules under a container rule into `order`, which must name each of them once. */
  def of(taskId: Identifier, spec: TransformSpec, parentId: Identifier, order: Seq[String]): ReorderMappings = {
    val parent = RuleTraverser(spec.mappingRule).find(parentId)
      .getOrElse(throw new NotFoundException(s"No rule '$parentId' found in transform '$taskId'."))
    val current = parent.operator.asInstanceOf[TransformRule].rules.propertyRules.map(_.id)
    if(order.sorted != current.map(_.toString).sorted) {
      throw BadUserInputException(s"Provided list [${order.mkString(", ")}] does not contain the same elements " +
        s"as current list [${current.mkString(", ")}].")
    }
    ReorderMappings(taskId, parentId, current, order.map(Identifier(_)))
  }
}

private object MappingChanges {

  /** The rule with the given id, which must be able to hold child rules. */
  def container(spec: TransformSpec, taskId: Identifier, ruleId: Identifier): RuleTraverser = {
    val traverser = rule(spec, taskId, ruleId)
    traverser.operator match {
      case _: ContainerTransformRule => traverser
      case _ => throw ChangeConflictException(s"Rule '$ruleId' in transform '$taskId' cannot hold child rules.")
    }
  }

  /** The rule with the given id. */
  def rule(spec: TransformSpec, taskId: Identifier, ruleId: Identifier): RuleTraverser = {
    RuleTraverser(spec.mappingRule).find(ruleId)
      .getOrElse(throw ChangeConflictException(s"No rule '$ruleId' found in transform '$taskId'."))
  }

  /** The rule with the id of `expected`, which must be unchanged. */
  def expectRule(spec: TransformSpec, taskId: Identifier, expected: TransformRule): RuleTraverser = {
    val traverser = rule(spec, taskId, expected.id)
    if(traverser.operator != expected) {
      throw ChangeConflictException(s"Rule '${expected.id}' in transform '$taskId' has been changed since.")
    }
    traverser
  }

  /** The transform with the tree of the traverser as its root rule. */
  def withRoot(spec: TransformSpec, traverser: RuleTraverser): TransformSpec = {
    spec.copy(mappingRule = traverser.root.operator.asInstanceOf[RootMappingRule])
  }
}
