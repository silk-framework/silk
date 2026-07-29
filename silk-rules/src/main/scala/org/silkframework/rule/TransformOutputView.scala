package org.silkframework.rule

import org.silkframework.entity.EntitySchema
import org.silkframework.entity.paths.{DirectionalPathOperator, TypedPath, UntypedPath}
import org.silkframework.rule.TransformOutputView.RulesAtTargetPath
import org.silkframework.rule.TransformSpec.RuleSchemata
import org.silkframework.rule.input.Input
import org.silkframework.runtime.validation.BadUserInputException
import org.silkframework.util.Identifier
import org.silkframework.util.Uri

/**
  * A view of the entities that a transform generates, i.e. its output as downstream tasks read it.
  * The rules of object rules without a target are folded into the rule whose entities they write into.
  * Execution and rule editing work on the raw rules instead.
  */
class TransformOutputView(spec: TransformSpec) {

  /** The rule schemata of the entity-generating rules, with the rules of object rules without a target folded in. */
  lazy val mergedRuleSchemata: Seq[RuleSchemata] = {
    val (noTargetSchemata, generators) = spec.ruleSchemata.partition(schemata =>
      schemata.transformRule.target.isEmpty && !schemata.transformRule.isInstanceOf[RootMappingRule])
    generators.map { generator =>
      val ruleIds = generator.transformRule.rules.allRulesRecursive.map(_.id).toSet
      // A no-target rule writes into the entities of the nearest generator above it, which shares its target path
      mergedSchemata(generator, noTargetSchemata.filter(schemata =>
        schemata.outputSchema.subPath == generator.outputSchema.subPath && ruleIds.contains(schemata.transformRule.id)))
    }
  }

  /**
    * The schemata of the rule that generates the given target type, if there is one.
    * A target type selects a single rule, unlike a target path: the entities of a selected type are retrieved from
    * one rule (see TransformTaskUtils.asDataSource), so the schema must not promise more than that rule generates.
    */
  def ruleSchemataForTargetTypeOption(targetType: Uri): Option[RuleSchemata] = {
    // The empty URI means that no type is selected, so it must not match a type rule with an empty URI
    if(targetType.uri.isEmpty) {
      None
    } else {
      mergedRuleSchemata.find(_.transformRule.rules.typeRules.map(_.typeUri).contains(targetType))
    }
  }

  /**
    * The schemata of the rule that generates the given target type.
    *
    * @throws BadUserInputException If no rule generates the given type.
    */
  def ruleSchemataForTargetType(targetType: Uri): RuleSchemata = {
    ruleSchemataForTargetTypeOption(targetType)
      .getOrElse(throw new BadUserInputException(s"No rule matching target type $targetType found."))
  }

  /** The schemata of the rule that generates the given target type, or of the root rule if no rule generates it. */
  def ruleSchemataForTargetTypeOrPrimary(targetType: Uri): RuleSchemata = {
    ruleSchemataForTargetTypeOption(targetType).getOrElse(mergedRuleSchemata.head)
  }

  /**
    * The output schema of the rule that generates the given target type, which is what a downstream task reads.
    * Falls back to the primary output type if no rule generates it, e.g. a type left over from a previous input.
    */
  def outputSchemaForTargetType(targetType: Uri): EntitySchema = {
    ruleSchemataForTargetTypeOrPrimary(targetType).outputSchema
  }

  /**
    * The rules that generate the entities at the given path, relative to the given rule's own target path.
    * A target property identifies a set of rules, not one: several rules may generate the same property.
    * Scoped to the given rule's tree, since a sibling rule may share the target path, but does not belong
    * to a source scoped to a single rule.
    */
  def rulesAtTargetPath(withinRule: TransformRule, relativePath: UntypedPath): RulesAtTargetPath = {
    val basePath = mergedRuleSchemata.find(_.transformRule.id == withinRule.id)
      .map(_.outputSchema.subPath).getOrElse(UntypedPath.empty)
    val targetPath = basePath ++ relativePath
    val subtreeSchemata = ruleSchemataWithinRule(withinRule)
    val rules = subtreeSchemata.filter(_.outputSchema.subPath == targetPath)
    if(rules.nonEmpty) {
      RulesAtTargetPath.Rules(rules)
    } else if(subtreeSchemata.exists(_.generatesValueAt(targetPath)) ||
              relativePath.operators.exists(!_.isInstanceOf[DirectionalPathOperator])) {
      RulesAtTargetPath.NoEntities(targetPath)
    } else {
      RulesAtTargetPath.NotGenerated(targetPath)
    }
  }

  /** The merged schemata of the given rule and all entity-generating rules nested below it. */
  def ruleSchemataWithinRule(rule: TransformRule): Seq[RuleSchemata] = {
    val ruleIds = (rule.rules.allRulesRecursive.map(_.id) :+ rule.id).toSet
    mergedRuleSchemata.filter(schemata => ruleIds.contains(schemata.transformRule.id))
  }

  /** Folds the rules of the given no-target rules into the generator whose entities they write into. */
  private def mergedSchemata(generator: RuleSchemata, noTargetSchemata: Seq[RuleSchemata]): RuleSchemata = {
    if(noTargetSchemata.isEmpty) {
      generator
    } else {
      val inlinedRules = for {
        schemata <- noTargetSchemata
        // The rules read relative to their own rule's source path, the merged rule relative to the generator's
        sourcePrefix = UntypedPath.removePathPrefix(schemata.inputSchema.subPath, generator.inputSchema.subPath)
        rule <- schemata.transformRule.rules.propertyRules if rule.target.isDefined
      } yield {
        inlinedRule(rule, sourcePrefix)
      }
      // The inlined rules may read paths that the generator's own input schema does not contain
      val extraPaths = inlinedRules.flatMap(_.sourcePaths).map(path => UntypedPath(path.operators).asStringTypedPath)
      RuleSchemata(
        transformRule = withReplacedRules(generator.transformRule, noTargetSchemata.map(_.transformRule.id).toSet, inlinedRules),
        inputSchema = withExtraPaths(generator.inputSchema, extraPaths),
        outputSchema = generator.outputSchema.copy(typedPaths =
          (generator.outputSchema.typedPaths ++ noTargetSchemata.flatMap(_.outputSchema.typedPaths)).distinctBy(_.toUntypedPath))
      )
    }
  }

  private def inlinedRule(rule: TransformRule, sourcePrefix: UntypedPath): TransformRule = {
    if(sourcePrefix.operators.isEmpty) {
      rule
    } else {
      ComplexMapping(rule.id, Input.rewriteSourcePaths(rule.operator, path => UntypedPath(sourcePrefix.operators ++ path.operators)),
        rule.target, rule.metaData)
    }
  }

  /** Replaces the no-target object rules by their inlined rules, so that all identifiers stay unique. */
  private def withReplacedRules(rule: TransformRule, removedIds: Set[Identifier], extraRules: Seq[TransformRule]): TransformRule = {
    def replaced(rules: MappingRules): MappingRules = {
      rules.copy(propertyRules = rules.propertyRules.filterNot(rule => removedIds.contains(rule.id)) ++ extraRules)
    }
    rule match {
      case rootRule: RootMappingRule => rootRule.copy(rules = replaced(rootRule.rules))
      case objectRule: ObjectMapping => objectRule.copy(rules = replaced(objectRule.rules))
      case other => other
    }
  }

  private def withExtraPaths(schema: EntitySchema, extraPaths: Seq[TypedPath]): EntitySchema = {
    val newPaths = extraPaths.filterNot(path => schema.typedPaths.exists(_.toUntypedPath == path.toUntypedPath)).distinct
    schema.copy(typedPaths = schema.typedPaths ++ newPaths)
  }
}

object TransformOutputView {

  /** The entities that a source scoped to a rule finds at a target path (see [[TransformOutputView.rulesAtTargetPath]]). */
  sealed trait RulesAtTargetPath
  object RulesAtTargetPath {
    /** The rules that generate entities at the path. */
    case class Rules(schemata: Seq[RuleSchemata]) extends RulesAtTargetPath
    /** The path holds no entities: it is generated as a value property, or it contains operators that cannot
      * address a rule, e.g. filters. Not an error. */
    case class NoEntities(targetPath: UntypedPath) extends RulesAtTargetPath
    /** No rule generates the path, which hints at a misconfigured source path. */
    case class NotGenerated(targetPath: UntypedPath) extends RulesAtTargetPath
  }
}
