package org.silkframework.rule

import org.silkframework.config.TaskSpec
import org.silkframework.entity._
import org.silkframework.execution.local.MultiEntityTable
import org.silkframework.rule.RootMappingRule.RootMappingRuleFormat
import org.silkframework.runtime.serialization.XmlSerialization._
import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat}
import org.silkframework.util.Identifier

import scala.xml.{Node, Null}

/**
  * This class contains all the required parameters to execute a transform task.
  *
  * @param selection          Selects the entities that are covered by this transformation.
  * @param mappingRule        The root mapping rule
  * @param outputs            The identifier of the output to which all transformed entities are to be written
  * @param errorOutputs       The identifier of the output to received erroneous entities.
  * @param targetVocabularies The URIs of the target vocabularies to which this transformation maps.
  * @since 2.6.1
  * @see org.silkframework.execution.ExecuteTransform
  */
case class TransformSpec(selection: DatasetSelection,
                         mappingRule: RootMappingRule,
                         outputs: Seq[Identifier] = Seq.empty,
                         errorOutputs: Seq[Identifier] = Seq.empty,
                         targetVocabularies: Traversable[String] = Seq.empty) extends TaskSpec {

  def rules: MappingRules = mappingRule.rules

  override def inputSchemataOpt: Option[Seq[EntitySchema]] = Some(Seq(inputSchema))

  override def outputSchemaOpt: Some[EntitySchema] = Some(outputSchema)

  override lazy val referencedTasks = Set(selection.inputId)

  lazy val inputSchema: EntitySchema = {
    val schemata = collectInputSchemata(mappingRule.rules, Path.empty)
    new MultiEntitySchema(schemata.head, schemata.tail)
  }

  lazy val outputSchema: EntitySchema = {
    val schemata = collectOutputSchemata(mappingRule.rules, Path.empty)
    new MultiEntitySchema(schemata.head, schemata.tail)
  }

  private def collectInputSchemata(rules: MappingRules, subPath: Path): Seq[EntitySchema] = {
    var schemata = Seq[EntitySchema]()

    schemata :+= EntitySchema(
      typeUri = selection.typeUri,
      typedPaths = rules.allRules.flatMap(_.paths).map(p => TypedPath(p, StringValueType)).distinct.toIndexedSeq,
      filter = selection.restriction,
      subPath = subPath
    )

    for(ObjectMapping(_, relativePath, _, childRules, _) <- rules.allRules) {
      schemata ++= collectInputSchemata(childRules, relativePath)
    }

    schemata
  }

  private def collectOutputSchemata(rules: MappingRules, subPath: Path): Seq[EntitySchema] = {
    var schemata = Seq[EntitySchema]()

    schemata :+= EntitySchema(
      typeUri = rules.typeRules.headOption.map(_.typeUri).getOrElse(selection.typeUri),
      typedPaths = rules.allRules.flatMap(_.target).map { mt =>
        val path = if (mt.isBackwardProperty) BackwardOperator(mt.propertyUri) else ForwardOperator(mt.propertyUri)
        TypedPath(Path(List(path)), mt.valueType)
      }.distinct.toIndexedSeq
    )

    for(ObjectMapping(_, relativePath, _, childRules, _) <- rules.allRules) {
      schemata ++= collectOutputSchemata(childRules, relativePath)
    }

    schemata
  }

  /**
    * Return the transform rule and the combined source path to that rule.
    * @param ruleName The ID of the rule.
    */
  def nestedRuleAndSourcePath(ruleName: String): Option[(TransformRule, List[PathOperator])] = {
    fetchRuleAndSourcePath(mappingRule, ruleName, List.empty)
  }

  // Recursively search for the rule in the transform spec rule tree and accumulate the source path.
  private def fetchRuleAndSourcePath(transformRule: TransformRule,
                                     ruleName: String,
                                     sourcePath: List[PathOperator]): Option[(TransformRule, List[PathOperator])] = {
    val sourcePathOperators = sourcePathOfRule(transformRule)
    if(transformRule.id.toString == ruleName) {
      Some(transformRule, sourcePath ::: sourcePathOperators) // Found the rule, return the result
    } else {
      transformRule.rules.flatMap(rule => fetchRuleAndSourcePath(rule, ruleName, sourcePath ::: sourcePathOperators )).headOption
    }
  }

  private def sourcePathOfRule(transformRule: TransformRule) = {
    transformRule match {
      case objMapping: ObjectMapping =>
        objMapping.sourcePath.operators
      case _ =>
        Nil
    }
  }
}

/**
  * Static functions for the TransformSpecification class.
  */
object TransformSpec {

  def empty: TransformSpec = TransformSpec(DatasetSelection.empty, RootMappingRule("root", MappingRules.empty))

  implicit object TransformSpecificationFormat extends XmlFormat[TransformSpec] {
    /**
      * Deserialize a value from XML.
      */
    override def read(node: Node)(implicit readContext: ReadContext): TransformSpec = {
      // Get the required parameters from the XML configuration.
      val datasetSelection = DatasetSelection.fromXML((node \ "SourceDataset").head)
      val sinks = (node \ "Outputs" \ "Output" \ "@id").map(_.text).map(Identifier(_))
      val errorSinks = (node \ "ErrorOutputs" \ "ErrorOutput" \ "@id").map(_.text).map(Identifier(_))
      val targetVocabularies = (node \ "TargetVocabularies" \ "Vocabulary").map(n => (n \ "@uri").text).filter(_.nonEmpty)

      val rootMappingRule = {
        // Stay compatible with the old format.
        val oldRules = (node \ "TransformRule" ++ node \ "ObjectMapping").map(fromXml[TransformRule])
        if(oldRules.nonEmpty) {
          RootMappingRule("root", MappingRules.fromSeq(oldRules))
        } else {
          RootMappingRuleFormat.read((node \ "RootMappingRule").head)
        }
      }

      // Create and return a TransformSpecification instance.
      TransformSpec(datasetSelection, rootMappingRule, sinks, errorSinks, targetVocabularies)
    }

    /**
      * Serialize a value to XML.
      */
    override def write(value: TransformSpec)(implicit writeContext: WriteContext[Node]): Node = {
      <TransformSpec>
        {value.selection.toXML(true)}
        {toXml(value.mappingRule)}
        <Outputs>
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

}