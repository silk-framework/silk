package org.silkframework.rule

import org.silkframework.config.TaskSpec
import org.silkframework.entity.{Path, _}
import org.silkframework.runtime.serialization.XmlSerialization._
import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat}
import org.silkframework.util.{Identifier, Uri}

import scala.xml.{Node, Null}
import TransformSpec._

/**
  * This class contains all the required parameters to execute a transform task.
  *
  * @param selection          Selects the entities that are covered by this transformation.
  * @param rules              The mapping rules
  * @param outputs            The identifier of the output to which all transformed entities are to be written
  * @param errorOutputs       The identifier of the output to received erroneous entities.
  * @param targetVocabularies The URIs of the target vocabularies to which this transformation maps.
  * @since 2.6.1
  * @see org.silkframework.execution.ExecuteTransform
  */
case class TransformSpec(selection: DatasetSelection,
                         rules: Seq[TransformRule],
                         outputs: Seq[Identifier] = Seq.empty,
                         errorOutputs: Seq[Identifier] = Seq.empty,
                         targetVocabularies: Traversable[String] = Seq.empty) extends TaskSpec {

  lazy val inputSchema: SchemaTrait = {
    val hms = hierarchicalMappings(rules)
    if (hms.isEmpty) {
      flatEntityInputSchema(rules)
    } else {
      NestedEntitySchema(rulesToNestedInputSchemaNode(rules))
    }
  }

  lazy val outputSchema: SchemaTrait = {
    val hms = hierarchicalMappings(rules)
    if (hms.isEmpty) {
      flatEntityOutputSchema(rules)
    } else {
      NestedEntitySchema(rulesToNestedOutputSchemaNode(rules))
    }
  }

  private def flatEntityOutputSchema(transformRules: Seq[TransformRule]) = {
    EntitySchema(
      typeUri = transformRules.collect { case tm: TypeMapping => tm.typeUri }.headOption.getOrElse(selection.typeUri),
      typedPaths = transformRules.flatMap(_.target).map(mt => TypedPath(Path(mt.propertyUri), mt.valueType)).toIndexedSeq
    )
  }

  override def inputSchemataOpt: Option[Seq[SchemaTrait]] = Some(Seq(inputSchema))

  override def outputSchemaOpt: Some[SchemaTrait] = Some(outputSchema)

  override lazy val referencedTasks = Set(selection.inputId)

  private def flatEntityInputSchema(transformRules: Seq[TransformRule]) = {
    EntitySchema(
      typeUri = selection.typeUri,
      // FIXME: Transform rule inputs are not typed, allow typed input paths? Until then use String value type.
      typedPaths = transformRules.flatMap(_.paths).map(p => TypedPath(p, StringValueType)).distinct.toIndexedSeq,
      filter = selection.restriction
    )
  }

  private def rulesToNestedOutputSchemaNode(rules: Seq[TransformRule]): NestedSchemaNode = {
    val flattenedRules = mergeRulesForOutputSchema(rules)
    flattenedRulesToNestedOutputSchema(flattenedRules)
  }

  private def flattenedRulesToNestedInputSchema(flattenedRules: FlattenedRulesInput): NestedSchemaNode = {
    val rules = flattenedRules.rules
    val flatSchema = flatEntityInputSchema(rules)
    val nestedSchema = for ((sourcePath, nested) <- flattenedRules.nestedRules) yield {
      val entitySchemaConnection = EntitySchemaConnection(sourcePath)
      (entitySchemaConnection, flattenedRulesToNestedInputSchema(nested))
    }
    NestedSchemaNode(flatSchema, nestedSchema.toIndexedSeq)
  }

  private def rulesToNestedInputSchemaNode(rules: Seq[TransformRule]): NestedSchemaNode = {
    val flattenedRules = mergeRulesForInputSchema(rules)
    flattenedRulesToNestedInputSchema(flattenedRules)
  }

  private def flattenedRulesToNestedOutputSchema(flattenedRules: FlattenedRulesOutput): NestedSchemaNode = {
    val rules = flattenedRules.rules
    val flatSchema = flatEntityOutputSchema(rules)
    val nestedSchema = for ((propertyUri, nested) <- flattenedRules.nestedRules) yield {
      val entitySchemaConnection = EntitySchemaConnection(Path(propertyUri))
      (entitySchemaConnection, flattenedRulesToNestedOutputSchema(nested))
    }
    NestedSchemaNode(flatSchema, nestedSchema.toIndexedSeq)
  }

  /** Merges rules for the output schema.
    * FIXME: Not working for all edge cases, e.g. two hierarchical mappings with empty target property that have the same nested entities.
    * Or two or more transitive hierarchical mappings with empty target property.
    */
  private def mergeRulesForOutputSchema(rules: Seq[TransformRule]): FlattenedRulesOutput = {
    val hms = hierarchicalMappings(rules)
    val (sameLevelMappings, deeperLevelNestedMappings) = hms.partition(sameTargetLevel)
    val fms = flatMappings(rules)
    val nestedFlatMappings = for (sameLevelMapping <- sameLevelMappings) yield {
      flatMappings(sameLevelMapping.childRules)
    }
    val flatRules = fms ++ nestedFlatMappings.flatten
    val deepFlattenedRules = deeperLevelNestedMappings map (d => (d.targetProperty.get, mergeRulesForOutputSchema(d.childRules)))
    FlattenedRulesOutput(flatRules, deepFlattenedRules)
  }

  /** Merges rules for the input schema.
    */
  def mergeRulesForInputSchema(rules: Seq[TransformRule]): FlattenedRulesInput = {
    val hms = hierarchicalMappings(rules)
    val fms = flatMappings(rules)
    val (sameLevelMappings, deeperLevelNestedMappings) = hms.partition(sameSourceLevel)
    val sameLevelMappingsFlattened = for (sameLevelMapping <- sameLevelMappings) yield {
      // Flatten recursively, so transitive child mappings with empty path get flattened to the current level
      val childFlattenedRules = mergeRulesForInputSchema(sameLevelMapping.childRules)
      (childFlattenedRules.rules, childFlattenedRules.nestedRules)
    }
    val flatRules = fms ++ sameLevelMappingsFlattened.flatMap(_._1)
    val deepRules = for (deepLevelMapping <- deeperLevelNestedMappings) yield {
      (deepLevelMapping.relativePath, mergeRulesForInputSchema(deepLevelMapping.childRules))
    }
    val allDeepRules = (deepRules ++ sameLevelMappingsFlattened.flatMap(_._2)).distinct
    FlattenedRulesInput(flatRules, allDeepRules)
  }
}

/**
  * A flattened and merged version of the transform rules of a transformation based on the hierarchical mapping target properties.
  */
case class FlattenedRulesOutput(rules: Seq[TransformRule],
                                // The target property to the nested resource and the nested rules
                                nestedRules: Seq[(Uri, FlattenedRulesOutput)])

/**
  * A flattened and merged version of the transform rules of a transformation based on the hierarchical mapping source path.
  */
case class FlattenedRulesInput(rules: Seq[TransformRule],
                               // The source path to the nested resource and the nested rules
                               nestedRules: Seq[(Path, FlattenedRulesInput)])

/**
  * Static functions for the TransformSpecification class.
  */
object TransformSpec {

  implicit object TransformSpecificationFormat extends XmlFormat[TransformSpec] {
    /**
      * Deserialize a value from XML.
      */
    override def read(node: Node)(implicit readContext: ReadContext): TransformSpec = {
      // Get the required parameters from the XML configuration.
      val datasetSelection = DatasetSelection.fromXML((node \ "SourceDataset").head)
      val rules = (node \ "TransformRule").map(fromXml[TransformRule])
      val sinks = (node \ "Outputs" \ "Output" \ "@id").map(_.text).map(Identifier(_))
      val errorSinks = (node \ "ErrorOutputs" \ "ErrorOutput" \ "@id").map(_.text).map(Identifier(_))
      val targetVocabularies = (node \ "TargetVocabularies" \ "Vocabulary").map(n => (n \ "@uri").text).filter(_.nonEmpty)

      // Create and return a TransformSpecification instance.
      TransformSpec(datasetSelection, rules, sinks, errorSinks, targetVocabularies)
    }

    /**
      * Serialize a value to XML.
      */
    override def write(value: TransformSpec)(implicit writeContext: WriteContext[Node]): Node = {
      <TransformSpec>
        {value.selection.toXML(true)}{value.rules.map(toXml[TransformRule])}<Outputs>
        {value.outputs.map(o => <Output id={o}></Output>)}
      </Outputs>{if (value.errorOutputs.isEmpty) {
        Null
      } else {
        <ErrorOutputs>
          {value.errorOutputs.map(o => <ErrorOutput id={o}></ErrorOutput>)}
        </ErrorOutputs>
      }}<TargetVocabularies>
        {for (targetVocabulary <- value.targetVocabularies) yield {
            <Vocabulary uri={targetVocabulary}/>
        }}
      </TargetVocabularies>
      </TransformSpec>
    }
  }

  def hierarchicalMappings(transformRules: Seq[TransformRule]): Seq[HierarchicalMapping] = {
    transformRules.collect { case hm: HierarchicalMapping => hm }
  }

  def flatMappings(transformRules: Seq[TransformRule]): Seq[TransformRule] = {
    transformRules.filter(!_.isInstanceOf[HierarchicalMapping])
  }

  // Mapping is on the same source level
  def sameSourceLevel(hierarchicalMapping: HierarchicalMapping): Boolean = {
    hierarchicalMapping.relativePath.operators.isEmpty // TODO: Is it really empty? Or an empty forward path?
  }

  def sameTargetLevel(hierarchicalMapping: HierarchicalMapping): Boolean = {
    hierarchicalMapping.targetProperty.isEmpty
  }
}