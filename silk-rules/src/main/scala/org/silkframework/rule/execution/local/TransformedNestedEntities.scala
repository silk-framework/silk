package org.silkframework.rule.execution.local

import java.util.logging.Logger

import org.silkframework.entity._
import org.silkframework.execution.ExecutionReport
import org.silkframework.rule.TransformSpec._
import org.silkframework.rule.{TransformRule, TransformSpec}
import org.silkframework.runtime.activity.ActivityContext
import org.silkframework.util.Uri

import scala.util.control.NonFatal

/**
  * Traversable that transforms entities of the nested source schema into entities of the nested target schema.
  */
class TransformedNestedEntities(entities: Traversable[NestedEntity],
                                transform: TransformSpec,
                                context: ActivityContext[ExecutionReport]) extends Traversable[NestedEntity] {
  // TODO: Rewrite for nested entities

  private val log: Logger = Logger.getLogger(this.getClass.getName)

  private def subjectRule(rules: Seq[TargetViewTransformRule]): Option[TargetViewTransformRule] = rules.find(isSubjectRule)

  private def isSubjectRule(rule: TargetViewTransformRule): Boolean = rule.transformRule.target.isEmpty

  private def propertyRules(rules: Seq[TargetViewTransformRule]): IndexedSeq[TargetViewTransformRule] = rules.filter(isPropertyRule).toIndexedSeq

  private def isPropertyRule(rule: TargetViewTransformRule): Boolean = rule.transformRule.target.nonEmpty

  //  FIXME: No transform report for now, what needs to change?
  //  private val report = new TransformReportBuilder(propertyRules)

  private var errorFlag = false

  private val nestedRootOutputSchema: NestedEntitySchema = transform.outputSchema
  private val nestedRootInputSchema: NestedEntitySchema = transform.inputSchema

  override def foreach[U](f: (NestedEntity) => U): Unit = {
    val targetViewSchema = mergeRulesForTargetViewTransformation(transform.rules)
    var count = 0
    var lastLog = System.currentTimeMillis()
    val inputSchema = nestedRootInputSchema.rootSchemaNode
    val outputSchema = nestedRootOutputSchema.rootSchemaNode
    for (entity <- entities) {
      //      errorFlag = false
      // For each schema path, collect all rules that map to it, the results of all rules will be merged
      val nestedEntity = transformNestedEntity(inputSchema, outputSchema, targetViewSchema, entity)
      f(nestedEntity)

      //      report.incrementEntityCounter()
      //      if (errorFlag)
      //        report.incrementEntityErrorCounter()


      count += 1
      if (count % 1000 == 0 && System.currentTimeMillis() > lastLog + 1000) {
        //        context.value.update(report.build())
        context.status.updateMessage(s"Executing ($count Entities)")
        lastLog = System.currentTimeMillis()
      }
    }
    //    context.value() = report.build()
  }

  /**
    * Transforms the input nested entity to the target nested entity according to the TransformSpec.
    *
    * @param inputSchema      The input schema for the nested entity
    * @param outputSchema     The output schema for the nested entity
    * @param targetViewSchema The transformation rules from a target schema perspective
    * @param entity           the nested entity
    * @return
    */
  private def transformNestedEntity(inputSchema: NestedSchemaNode,
                                    outputSchema: NestedSchemaNode,
                                    targetViewSchema: TargetViewSchema,
                                    entity: NestedEntity): NestedEntity = {
    val rulesPerProperty = groupRulesByTargetProperty(outputSchema, targetViewSchema)
    val subjectRule = targetViewSchema.subjectRule
    val (uri, values) = executeRules(entity, rulesPerProperty, inputSchema, subjectRule)
    val transformedChildren = for ((targetViewConnection, (_, nestedOutputSchema)) <- targetViewSchema.nestedRules.
        zip(outputSchema.nestedEntities)) yield {
      val nestedTargetViewSchema = targetViewConnection.targetViewSchema
      val (nextEntities, nextInputSchema) = traverseSourceEntity(inputSchema, entity, targetViewConnection)
      for (childEntity <- nextEntities) yield {
        transformNestedEntity(nextInputSchema, nestedOutputSchema, nestedTargetViewSchema, childEntity)
      }
    }
    NestedEntity(uri, values, transformedChildren.toIndexedSeq)
  }

  private def groupRulesByTargetProperty(outputSchema: NestedSchemaNode, targetViewSchema: TargetViewSchema) = {
    for (path <- outputSchema.entitySchema.typedPaths.map(_.path)) yield {
      path.propertyUri match {
        case Some(property) =>
          targetViewSchema.propertyRules.filter(_.transformRule.target.get.propertyUri == property)
        case None =>
          IndexedSeq.empty
      }
    }
  }

  /** Returns the entities that the nested rules should be applied on */
  private def traverseSourceEntity(inputSchema: NestedSchemaNode, entity: NestedEntity, targetViewConnection: TargetViewConnection) = {
    val path = targetViewConnection.sourcePath
    path.operators match {
      case Nil | List(ForwardOperator(Uri(""))) =>
        // Stay on this entity and input schema
        (Seq(entity), inputSchema)
      case _ =>
        // find nested input entities by source path
        val idx = inputSchema.nestedEntities.indexWhere(_._1.path == path)
        assert(idx > -1, s"Source connection path $path was not found in source entity schema. Available paths: " +
            inputSchema.nestedEntities.map(_._1.path.toString).mkString(", "))
        (entity.nestedEntities(idx), inputSchema.nestedEntities(idx)._2)
    }
  }

  private def nestedEntityToFlatEntity[U](entity: NestedEntity, inputSchema: NestedSchemaNode): Entity = {
    new Entity(entity.uri, entity.values, inputSchema.entitySchema)
  }

  /**
    * Returns a tuple of the entity URI and the entity's transformed values.
    *
    * @param entity           the nested entity to process
    * @param rulesPerProperty the rules that should be executed for a specific target property
    * @param inputSchema      the nested input schema for the current entity
    * @param subjectRule      the optional subject rule to generate the entity URI
    */
  private def executeRules[U](entity: NestedEntity,
                              rulesPerProperty: IndexedSeq[Seq[TargetViewTransformRule]],
                              inputSchema: NestedSchemaNode,
                              subjectRule: Option[TargetViewTransformRule]): (String, IndexedSeq[Seq[String]]) = {
    val flatEntity = nestedEntityToFlatEntity(entity, inputSchema)
    val uri = subjectRule.flatMap(rule => rule.transformRule(flatEntity).headOption).getOrElse(entity.uri)
    val values =
      for (rules <- rulesPerProperty) yield {
        rules.flatMap { rule =>
          rule.sourcePath.operators match {
            case Nil | List(ForwardOperator(Uri(""))) =>
              evaluateRule(flatEntity)(rule.transformRule)
            case _ =>
              val idx = inputSchema.nestedEntities.indexWhere(_._1.path == rule.sourcePath)
              assert(idx > -1, s"No nested entity found with source path ${rule.sourcePath}! Available paths: " +
                  inputSchema.nestedEntities.map(_._1.path.toString).mkString(", "))
              val nestedInputSchema = inputSchema.nestedEntities(idx)._2
              val vs = for (entity <- entity.nestedEntities(idx)) yield {
                val flatNestedEntity = nestedEntityToFlatEntity(entity, nestedInputSchema)
                evaluateRule(flatNestedEntity)(rule.transformRule)
              }
              vs.flatten
          }
        }
      }
    (uri, values)
  }

  private def evaluateRule(entity: Entity)(rule: TransformRule): Seq[String] = {
    try {
      rule(entity)
    } catch {
      case NonFatal(ex) =>
        log.fine("Error during execution of transform rule " + rule.name.toString + ": " + ex.getMessage)
        //          report.addError(rule, entity, ex)
        errorFlag = true
        Seq.empty
    }
  }

  /** Orders the transform rules in a way that they can easily be executed to construct entities according to the output schema.
    * FIXME: Not working for all edge cases, e.g. two hierarchical mappings with empty target property that have the same nested entities.
    */
  private def mergeRulesForTargetViewTransformation(rules: Seq[TransformRule]): TargetViewSchema = {
    val hms = hierarchicalMappings(rules)
    val (sameLevelMappings, deeperLevelNestedMappings) = hms.partition(sameTargetLevel)
    val fms = flatMappings(rules) map (rule => TargetViewTransformRule(Path.Empty, rule))
    val nestedFlatMappings = (for (sameLevelMapping <- sameLevelMappings) yield {
      flatMappings(sameLevelMapping.childRules) map (rule => TargetViewTransformRule(sameLevelMapping.relativePath, rule))
    }).flatten
    val flatRules = fms ++ nestedFlatMappings
    val deepFlattenedRules = deeperLevelNestedMappings map (d =>
      TargetViewConnection(d.relativePath, d.targetProperty.get, mergeRulesForTargetViewTransformation(d.childRules)))
    TargetViewSchema(subjectRule(flatRules), propertyRules(flatRules), deepFlattenedRules)
  }
}

/**
  * The transformation rules from the target schema perspective.
  *
  * @param subjectRule   An optional entity URI rule carrying the relative source path information
  * @param propertyRules The transformation rules carrying the relative source path information
  * @param nestedRules   deeper nested rules according to the output schema
  */
case class TargetViewSchema(subjectRule: Option[TargetViewTransformRule],
                            propertyRules: Seq[TargetViewTransformRule],
                            // The target property to the nested resource and the nested rules
                            nestedRules: Seq[TargetViewConnection])

case class TargetViewConnection(sourcePath: Path, targetUri: Uri, targetViewSchema: TargetViewSchema)

/**
  * A transformation rule from a target view perspective.
  *
  * @param sourcePath    The source path to get to the source place where this rule should be executed.
  * @param transformRule The actual transform rule to execute
  */
case class TargetViewTransformRule(sourcePath: Path, transformRule: TransformRule)