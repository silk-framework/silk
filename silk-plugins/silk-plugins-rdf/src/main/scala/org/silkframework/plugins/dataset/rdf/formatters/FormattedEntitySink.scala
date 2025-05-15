package org.silkframework.plugins.dataset.rdf.formatters

import org.silkframework.config.Prefixes
import org.silkframework.dataset.{EntitySink, TripleSink, TypedProperty}
import org.silkframework.entity.ValueType
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.WritableResource
import org.silkframework.util.Uri

import java.io._

/**
 * An entity sink that writes formatted entity output to an output resource.
 */
class FormattedEntitySink(resource: WritableResource, formatter: EntityFormatter) extends EntitySink with TripleSink {

  private var properties = Seq[TypedProperty]()

  private var writer: Writer = _

  override def openTable(typeUri: Uri, properties: Seq[TypedProperty], singleEntity: Boolean = false)
                        (implicit userContext: UserContext, prefixes: Prefixes): Unit = {
    this.properties = properties
    if(writer == null) {
      val outputStream = resource.createOutputStream(append = true)
      writer = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"))
      //Write header
      writer.write(formatter.header)
    }
  }

  override def writeEntity(subject: String, values: IndexedSeq[Seq[String]])
                          (implicit userContext: UserContext): Unit = {
    for((property, valueSet) <- properties zip values;
        value <- valueSet) {
      if(property.isBackwardProperty) {
        writeStatement(value, property.propertyUri, subject, property.valueType)
      } else {
        writeStatement(subject, property.propertyUri, value, property.valueType)
      }
    }
  }

  private def writeStatement(subject: String, predicate: String, value: String, valueType: ValueType): Unit = {
    writer.write(formatter.formatLiteralStatement(subject, predicate, value, valueType))
  }

  override def closeTable()(implicit userContext: UserContext): Unit = {}

  override def close()(implicit userContext: UserContext): Unit = {
    if (Option(writer).isDefined) {
      try {
        writer.write(formatter.footer)
      } finally {
        writer.close()
        writer = null
      }
    }
  }

  override def init()(implicit userContext: UserContext, prefixes: Prefixes): Unit = {
    implicit val prefixes: Prefixes = Prefixes.empty
    openTable(typeUri = "", properties = Seq())
  }

  override def writeTriple(subject: String, predicate: String, value: String, valueType: ValueType)
                          (implicit userContext: UserContext): Unit = {
    writeStatement(subject, predicate, value, valueType)
  }

  /**
    * Makes sure that the next write will start from an empty dataset.
    */
  override def clear()(implicit userContext: UserContext): Unit = resource.delete()
}