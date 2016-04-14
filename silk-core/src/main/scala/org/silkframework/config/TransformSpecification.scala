package org.silkframework.config

import org.silkframework.entity.EntitySchema
import org.silkframework.rule.TransformRule
import org.silkframework.runtime.resource.ResourceManager
import org.silkframework.runtime.serialization.Serialization._
import org.silkframework.runtime.serialization.XmlFormat
import org.silkframework.util.Identifier

import scala.xml.{Null, Node}

/**
  * This class contains all the required parameters to execute a transform task.
  *
  * @since 2.6.1
  * @see org.silkframework.execution.ExecuteTransform
  */
case class TransformSpecification(id: Identifier = Identifier.random, selection: DatasetSelection, rules: Seq[TransformRule], outputs: Seq[Identifier] = Seq.empty, errorOutputs: Seq[Identifier] = Seq.empty) {

  def entitySchema = {
    EntitySchema(
      typeUri = selection.typeUri,
      paths = rules.flatMap(_.paths).distinct.toIndexedSeq,
      filter = selection.restriction
    )
  }

}

/**
  * Static functions for the TransformSpecification class.
  */
object TransformSpecification {

  implicit object TransformSpecificationFormat extends XmlFormat[TransformSpecification] {
    /**
      * Deserialize a value from XML.
      */
    override def read(node: Node)(implicit prefixes: Prefixes, resources: ResourceManager): TransformSpecification = {
      // Get the Id.
      val id = (node \ "@id").text

      // Get the required parameters from the XML configuration.
      val datasetSelection = DatasetSelection.fromXML((node \ "SourceDataset").head)
      val rules = (node \ "TransformRule").map(fromXml[TransformRule])
      val sinks = (node \ "Outputs" \ "Output" \ "@id").map(_.text).map(Identifier(_))
      val errorSinks = (node \ "ErrorOutputs" \ "ErrorOutput" \ "@id").map(_.text).map(Identifier(_))

      // Create and return a TransformSpecification instance.
      TransformSpecification(id, datasetSelection, rules, sinks, errorSinks)
    }

    /**
      * Serialize a value to XML.
      */
    override def write(value: TransformSpecification)(implicit prefixes: Prefixes): Node = {
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