package org.silkframework.rule

import java.util.NoSuchElementException

import org.silkframework.config.Task.TaskFormat
import org.silkframework.config.{MetaData, Prefixes, Task, TaskSpec}
import org.silkframework.entity._
import org.silkframework.rule.RootMappingRule.RootMappingRuleFormat
import org.silkframework.rule.TransformSpec.RuleSchemata
import org.silkframework.runtime.serialization.XmlSerialization._
import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat}
import org.silkframework.runtime.validation.NotFoundException
import org.silkframework.util.{Identifier, IdentifierGenerator}

import scala.util.Try
import scala.xml.{Node, Null}
import scala.language.implicitConversions

/**
  * This class contains all the required parameters to execute a transform task.
  *
  * @param selection          Selects the entities that are covered by this transformation.
  * @param mappingRule        The root mapping rule
  * @param outputs            The identifier of the output to which all transformed entities are to be written
  * @param errorOutputs       The identifier of the output to received erroneous entities.
  * @param targetVocabularies The URIs of the target vocabularies to which this transformation maps.
  * @param parentTask     May add additional references to tasks (in addition to input and output)
  * @since 2.6.1
  * @see org.silkframework.execution.ExecuteTransform
  */
case class TransformSpec(selection: DatasetSelection,
                         mappingRule: RootMappingRule,
                         outputs: Seq[Identifier] = Seq.empty,
                         errorOutputs: Seq[Identifier] = Seq.empty,
                         targetVocabularies: Traversable[String] = Seq.empty,
                         parentTask: Option[Identifier] = None
                        ) extends TaskSpec {

  /** Retrieves the root rules of this transform spec. */
  def rules: MappingRules = mappingRule.rules

  /**
    * Retrieves a rule by its identifier.
    * Searches in the entire rule tree.
    *
    * @throws NoSuchElementException If no rule with the given identifier could be found.
    */
  def ruleById(ruleId: Identifier): TransformRule = {
    nestedRuleAndSourcePath(ruleId)
      .getOrElse(throw new NoSuchElementException(s"No rule with identifier 'ruleId' has been found."))
      ._1
  }

  override def inputSchemataOpt: Option[Seq[EntitySchema]] = Some(Seq(inputSchema))

  override def outputSchemaOpt: Some[EntitySchema] = Some(outputSchema)

  /**
    * The tasks that this task reads from.
    */
  override def inputTasks: Set[Identifier] = Set(selection.inputId)

  /**
    * The tasks that this task writes to.
    */
  override def outputTasks: Set[Identifier] = outputs.toSet

  /**
    * The tasks that are directly referenced by this task.
    * This includes input tasks and output tasks.
    */
  override def referencedTasks: Set[Identifier] = inputTasks ++ outputTasks ++ parentTask

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
    new MultiEntitySchema(ruleSchemata.head.inputSchema, ruleSchemata.tail.map(_.inputSchema).toIndexedSeq)
  }


  /**
    * Output schemata of all object rules in the tree.``
    */
  lazy val outputSchema: MultiEntitySchema = {
    new MultiEntitySchema(ruleSchemata.head.outputSchema, ruleSchemata.tail.map(_.outputSchema).toIndexedSeq)
  }

  /** Retrieves a list of properties as key-value pairs for this task to be displayed to the user. */
  override def properties(implicit prefixes: Prefixes): Seq[(String, String)] = {
    Seq(
      ("Source", selection.inputId.toString),
      ("Type", selection.typeUri.toString),
      ("Restriction", selection.restriction.toString)
    )
  }

  /**
    * Collects the input and output schemata of all rules recursively.
    */
  private def collectSchemata(rule: TransformRule, subPath: Path): Seq[RuleSchemata] = {
    var schemata = Seq[RuleSchemata]()

    // Add rule schemata for this rule
    schemata :+= RuleSchemata.create(rule, selection, subPath)

    // Add rule schemata of all child object rules
    for(objectMapping @ ObjectMapping(_, relativePath, _, _, _) <- rule.rules.allRules) {
      schemata ++= collectSchemata(objectMapping.fillEmptyUriRule, subPath ++ relativePath)
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

case class TransformTask(id: Identifier, data: TransformSpec, metaData: MetaData = MetaData.empty) extends Task[TransformSpec]

/**
  * Static functions for the TransformSpecification class.
  */
object TransformSpec {

  implicit def toTransformTask(task: Task[TransformSpec]): TransformTask = TransformTask(task.id, task.data, task.metaData)

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
        typedPaths = extractTypedPaths(rule),
        filter = selection.restriction,
        subPath = subPath
      )

      val outputSchema = EntitySchema(
        typeUri = rule.rules.typeRules.headOption.map(_.typeUri).getOrElse(selection.typeUri),
        typedPaths = rule.rules.allRules.flatMap(_.target).map { mt =>
          val path = if (mt.isBackwardProperty) BackwardOperator(mt.propertyUri) else ForwardOperator(mt.propertyUri)
          TypedPath(Path(List(path)), mt.valueType, mt.isAttribute)
        }.distinct.toIndexedSeq
      )

      RuleSchemata(rule, inputSchema, outputSchema)
    }
  }

  private def extractTypedPaths(rule: TransformRule): IndexedSeq[TypedPath] = {
    val rules = rule.rules.allRules
    val (objectRulesWithDefaultPattern, valueRules) = rules.partition ( _.representsDefaultUriRule )
    val valuePaths = valueRules.flatMap(_.sourcePaths).map(p => TypedPath(p, StringValueType, isAttribute = false)).distinct.toIndexedSeq
    val objectPaths = objectRulesWithDefaultPattern.flatMap(_.sourcePaths).map(p => TypedPath(p, UriValueType, isAttribute = false)).distinct.toIndexedSeq

    /** Value paths must come before object paths to not, because later algorithms rely on this order, e.g. PathInput only considers the Path not the value type.
      * If an object type path would come before the value path, the path input would take the wrong values. The other way round
      * is taken care of.
      */
    valuePaths ++ objectPaths
  }

  implicit object TransformSpecFormat extends XmlFormat[TransformSpec] {

    override def tagNames: Set[String] = Set("TransformSpec")

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
          (node \ "RootMappingRule").headOption match {
            case Some(node) =>
              RootMappingRuleFormat.read(node)
            case None =>
              RootMappingRule.empty
          }
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

  implicit object TransformTaskXmlFormat extends XmlFormat[TransformTask] {
    override def read(value: Node)(implicit readContext: ReadContext): TransformTask = {
      new TaskFormat[TransformSpec].read(value)
    }

    override def write(value: TransformTask)(implicit writeContext: WriteContext[Node]): Node = {
      new TaskFormat[TransformSpec].write(value)
    }
  }

  /** Creates an ID generator pre-populated with IDs from the transform spec */
  def identifierGenerator(transformSpec: TransformSpec): IdentifierGenerator = {
    val identifierGenerator = new IdentifierGenerator()
    for(id <- RuleTraverser(transformSpec.mappingRule).iterateAllChildren.map(_.operator.id)) {
      identifierGenerator.add(id)
    }
    identifierGenerator
  }
}