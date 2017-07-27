package org.silkframework.rule

import org.silkframework.config.TaskSpec
import org.silkframework.entity._
import org.silkframework.rule.RootMappingRule.RootMappingRuleFormat
import org.silkframework.rule.TransformSpec.RuleSchemata
import org.silkframework.runtime.serialization.XmlSerialization._
import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat}
import org.silkframework.runtime.validation.NotFoundException
import org.silkframework.util.{Identifier, Uri}

import scala.util.Try
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

  /** Retrieves the root rules of this transform spec. */
  def rules: MappingRules = mappingRule.rules

  override def inputSchemataOpt: Option[Seq[EntitySchema]] = Some(Seq(inputSchema))

  override def outputSchemaOpt: Some[EntitySchema] = Some(outputSchema)

  override lazy val referencedTasks = Set(selection.inputId)

  /**
    * Input and output schemata of all rules in the tree.
    */
  lazy val ruleSchemata: Seq[RuleSchemata] = {
    collectSchemata(mappingRule, Path.empty)
  }

  /**
    * Input schemata of all rules in the tree.
    */
  lazy val inputSchema: MultiEntitySchema = {
    new MultiEntitySchema(ruleSchemata.head.inputSchema, ruleSchemata.tail.map(_.inputSchema))
  }


  /**
    * Output schemata of all rules in the tree.
    */
  lazy val outputSchema: MultiEntitySchema = {
    new MultiEntitySchema(ruleSchemata.head.outputSchema, ruleSchemata.tail.map(_.outputSchema))
  }

  /**
    * Collects the input and output schemata of all rules recursively.
    */
  private def collectSchemata(rule: TransformRule, subPath: Path): Seq[RuleSchemata] = {
    var schemata = Seq[RuleSchemata]()

    // Add rule schemata for this rule
    schemata :+= RuleSchemata.create(rule, selection, subPath)

    // Add rule schemata of all child rules
    val objectMappings = rule.rules.allRules.collect { case m: ObjectMapping => m }
    for (objectMapping <- objectMappings) {
      schemata ++= collectSchemata(objectMapping, subPath ++ objectMapping.sourcePath)
    }

    schemata
  }

  /**
    * Return the transform rule and the combined source path to that rule.
    *
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
    if (transformRule.id.toString == ruleName) {
      Some(transformRule, sourcePath ::: sourcePathOperators) // Found the rule, return the result
    } else {
      transformRule.rules.flatMap(rule => fetchRuleAndSourcePath(rule, ruleName, sourcePath ::: sourcePathOperators)).headOption
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

  /** Return an entity schema for one specific rule of the transform specification */
  def oneRuleEntitySchemaById(ruleId: String,
                              typeUri: Option[String] = None,
                              restriction: Restriction = Restriction.empty): Try[RuleSchemata] = {
    Try {
      ruleSchemata.find(_.transformRule.id == ruleId) match {
        case Some(rule) =>
          rule
        case None =>
          val ruleIds = ruleSchemata.map(_.transformRule.id).sorted.mkString(", ")
          throw new NotFoundException(s"No rule with ID '$ruleId' found! Value rule IDs: $ruleIds")
      }
    }
  }
}

/**
  * Static functions for the TransformSpecification class.
  */
object TransformSpec {

  def empty: TransformSpec = TransformSpec(DatasetSelection.empty, RootMappingRule("root", MappingRules.empty))

  /**
    * Holds a transform rule along with its input and output schema.
    */
  case class RuleSchemata(transformRule: TransformRule, inputSchema: EntitySchema, outputSchema: EntitySchema)

  object RuleSchemata {
    def create(rule: TransformRule, selection: DatasetSelection, subPath: Path): RuleSchemata = {
      val inputSchema = EntitySchema(
        typeUri = selection.typeUri,
        typedPaths = rule.rules.allRules.flatMap(_.paths).map(p => TypedPath(p, StringValueType)).distinct.toIndexedSeq,
        filter = selection.restriction,
        subPath = subPath
      )

      val outputSchema = EntitySchema(
        typeUri = rule.rules.typeRules.headOption.map(_.typeUri).getOrElse(selection.typeUri),
        typedPaths = rule.rules.allRules.flatMap(_.target).map { mt =>
          val path = if (mt.isBackwardProperty) BackwardOperator(mt.propertyUri) else ForwardOperator(mt.propertyUri)
          TypedPath(Path(List(path)), mt.valueType)
        }.distinct.toIndexedSeq
      )

      RuleSchemata(rule, inputSchema, outputSchema)
    }
  }

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
        if (oldRules.nonEmpty) {
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
        {value.selection.toXML(true)}{toXml(value.mappingRule)}<Outputs>
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