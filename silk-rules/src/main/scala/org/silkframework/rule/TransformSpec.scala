package org.silkframework.rule

import org.silkframework.config.TaskSpec
import org.silkframework.entity._
import org.silkframework.execution.local.MultiEntityTable
import org.silkframework.runtime.serialization.XmlSerialization._
import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat}
import org.silkframework.util.Identifier

import scala.xml.{Node, Null}

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

  override def inputSchemataOpt: Option[Seq[EntitySchema]] = Some(Seq(inputSchema))

  override def outputSchemaOpt: Some[EntitySchema] = Some(outputSchema)

  override lazy val referencedTasks = Set(selection.inputId)

  lazy val inputSchema: EntitySchema = {
    val schemata = collectInputSchemata(rules, Path.empty)
    new MultiEntitySchema(schemata.head, schemata.tail)
  }

  lazy val outputSchema: EntitySchema = {
    val schemata = collectOutputSchemata(rules, Path.empty)
    new MultiEntitySchema(schemata.head, schemata.tail)
  }

  private def collectInputSchemata(rules: Seq[TransformRule], subPath: Path): Seq[EntitySchema] = {
    var schemata = Seq[EntitySchema]()

    schemata :+= EntitySchema(
      typeUri = selection.typeUri,
      typedPaths = rules.flatMap(_.paths).map(p => TypedPath(p, StringValueType)).distinct.toIndexedSeq,
      filter = selection.restriction,
      subPath = subPath
    )

    for(HierarchicalMapping(_, relativePath, _, childRules) <- rules) {
      schemata ++= collectInputSchemata(childRules, relativePath)
    }

    schemata
  }

  private def collectOutputSchemata(rules: Seq[TransformRule], subPath: Path): Seq[EntitySchema] = {
    var schemata = Seq[EntitySchema]()

    schemata :+= EntitySchema(
      typeUri = rules.collect { case tm: TypeMapping => tm.typeUri }.headOption.getOrElse(selection.typeUri),
      typedPaths = rules.flatMap(_.target).map(mt => TypedPath(Path(mt.propertyUri), mt.valueType)).toIndexedSeq
    )

    for(HierarchicalMapping(_, relativePath, _, childRules) <- rules) {
      schemata ++= collectOutputSchemata(childRules, relativePath)
    }

    schemata
  }

}

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

}