package org.silkframework.rule

import org.silkframework.config.TaskSpec
import org.silkframework.entity._
import org.silkframework.rule.RootMappingRule.RootMappingRuleFormat
import org.silkframework.rule.TransformSpec.RuleSchemata
import org.silkframework.runtime.serialization.XmlSerialization._
import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat}
import org.silkframework.runtime.validation.NotFoundException
import org.silkframework.util.Identifier

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
    * Input and output schemata of all object rules in the tree.
    */
  lazy val ruleSchemata: Seq[RuleSchemata] = {
    collectSchemata(mappingRule, Path.empty)
  }

  /**
    * Input schemata of all object rules in the tree.
    */
  lazy val inputSchema: MultiEntitySchema = {
    new MultiEntitySchema(ruleSchemata.head.inputSchema, ruleSchemata.tail.map(_.inputSchema))
  }


  /**
    * Output schemata of all object rules in the tree.
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
  def oneRuleEntitySchemaById(ruleId: Identifier): Try[RuleSchemata] = {
    Try {
      ruleSchemata.flatMap(_.hasMapping(ruleId)).headOption match {
        case Some(rule) =>
          rule
        case None =>
          val ruleIds = validRuleNames(mappingRule).sorted.mkString(", ")
          throw new NotFoundException(s"No rule with ID '$ruleId' found! Value rule IDs: $ruleIds")
      }
    }
  }

  def validRuleNames(mappingRule: TransformRule): List[String] = {
    val childRuleNames = mappingRule.rules.map(validRuleNames).foldLeft(List.empty[String])((l, ruleNames) => ruleNames ::: l)
    mappingRule.id :: childRuleNames
  }

  /** A list of relative source paths fetched recursively from the whole rule tree starting with the given rule by the rule id.
    * Only source paths are considered that are used in value property mappings, i.e. that have a target property set and
    * create a property value. Also, source paths from complex rules are only considered if they are the only source paths
    * in that rule. */
  def valueSourcePaths(ruleName: Identifier,
                       maxPathDepth: Int = Int.MaxValue): Seq[Path] = {
    nestedRuleAndSourcePath(ruleName) match {
      case Some((rule, _)) =>
        rule.children flatMap (child => valueSourcePathsRecursive(child, List.empty, maxPathDepth))
      case None =>
        throw new RuntimeException("No rule with name " + ruleName + " exists!")
    }
  }

  private def valueSourcePathsRecursive(rule: Operator,
                                        basePath: List[PathOperator],
                                        maxPathDepth: Int): Seq[Path] = {
    rule match {
      case transformRule: TransformRule =>
        transformRule match {
          case dm: DirectMapping =>
            listPath(basePath ++ dm.sourcePath.operators)
          case cm: ComplexMapping =>
            cm.sourcePaths match {
              case oneInput :: Nil =>
                listPath(basePath ++ oneInput.operators) // Only consider complex mapping with one single source path
              case _ =>
                List.empty
            }
          case om: ObjectMapping =>
            val newBasePath = basePath ++ om.sourcePath.operators
            om.children flatMap (c => valueSourcePathsRecursive(c, newBasePath, maxPathDepth))
          case rm: RootMappingRule =>
            rm.children flatMap (c => valueSourcePathsRecursive(c, List.empty, maxPathDepth)) // At the moment this branch is never taken
          case _: UriMapping | _: TypeMapping =>
            List.empty // Don't consider non-value mappings
        }
      case _ =>
        List() // No transform rule, no path used
    }
  }

  private def listPath(pathOperators: List[PathOperator]) =  List(Path(pathOperators))
}

/**
  * Static functions for the TransformSpecification class.
  */
object TransformSpec {

  def empty: TransformSpec = TransformSpec(DatasetSelection.empty, RootMappingRule("root", MappingRules.empty))

  /**
    * Holds a transform rule along with its input and output schema.
    */
  case class RuleSchemata(transformRule: TransformRule, inputSchema: EntitySchema, outputSchema: EntitySchema) {

    def hasMapping(ruleId: Identifier): Option[RuleSchemata] = {
      if(ruleId == transformRule.id) {
        Some(this)
      } else {
        for(childRule <- transformRule.rules.allRules.find(c => c.isInstanceOf[ValueTransformRule] && c.id == ruleId)) yield {
          val inputPaths = childRule.sourcePaths.map(_.asStringTypedPath).toIndexedSeq
          val outputPaths = childRule.target.map(t => Path(t.propertyUri).asStringTypedPath).toIndexedSeq
          RuleSchemata(
            transformRule = childRule,
            inputSchema = inputSchema.copy(typedPaths = inputPaths),
            outputSchema = outputSchema.copy(typedPaths = outputPaths)
          )
        }
      }
    }
  }

  object RuleSchemata {
    def create(rule: TransformRule, selection: DatasetSelection, subPath: Path): RuleSchemata = {
      val inputSchema = EntitySchema(
        typeUri = selection.typeUri,
        typedPaths = rule.rules.allRules.flatMap(_.sourcePaths).map(p => TypedPath(p, StringValueType)).distinct.toIndexedSeq,
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