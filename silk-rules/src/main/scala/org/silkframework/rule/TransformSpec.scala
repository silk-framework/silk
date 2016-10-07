package org.silkframework.rule

import org.silkframework.config.TaskSpec
import org.silkframework.entity.{EntitySchema, Path}
import org.silkframework.runtime.serialization.XmlSerialization._
import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat}
import org.silkframework.util.Identifier

import scala.xml.{Node, Null}

/**
  * This class contains all the required parameters to execute a transform task.
  *
  * @since 2.6.1
  * @see org.silkframework.execution.ExecuteTransform
  */
case class TransformSpec(selection: DatasetSelection,
                         rules: Seq[TransformRule],
                         outputs: Seq[Identifier] = Seq.empty,
                         errorOutputs: Seq[Identifier] = Seq.empty) extends TaskSpec {

  def entitySchema: EntitySchema = {
    EntitySchema(
      typeUri = selection.typeUri,
      paths = rules.flatMap(_.paths).distinct.toIndexedSeq,
      filter = selection.restriction
    )
  }

  override lazy val inputSchemataOpt: Option[Seq[EntitySchema]] = Some(Seq(entitySchema))

  override lazy val outputSchemaOpt: Some[EntitySchema] = {
    Some(
      EntitySchema(
        typeUri = rules.collect { case tm: TypeMapping => tm.typeUri }.headOption.getOrElse(selection.typeUri),
        paths = rules.flatMap(_.target).map(Path(_)).toIndexedSeq
      )
    )
  }

  override lazy val referencedTasks =  Set(selection.inputId)

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

      // Create and return a TransformSpecification instance.
      TransformSpec(datasetSelection, rules, sinks, errorSinks)
    }

    /**
      * Serialize a value to XML.
      */
    override def write(value: TransformSpec)(implicit writeContext: WriteContext[Node]): Node = {
      <TransformSpec>
        {value.rules.map(toXml[TransformRule])}<Outputs>
        {value.outputs.map(o => <Output id={o}></Output>)}
      </Outputs>{if (value.errorOutputs.isEmpty) {
        Null
      } else {
        <ErrorOutputs>
          {value.errorOutputs.map(o => <ErrorOutput id={o}></ErrorOutput>)}
        </ErrorOutputs>
      }}
      </TransformSpec>
    }
  }

}