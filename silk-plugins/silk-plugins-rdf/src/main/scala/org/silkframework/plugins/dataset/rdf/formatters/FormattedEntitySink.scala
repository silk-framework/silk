package org.silkframework.plugins.dataset.rdf.formatters

import java.io._

import org.silkframework.dataset.{TripleSink, EntitySink}
import org.silkframework.runtime.resource.{FileResource, WritableResource}

/**
 * Created by andreas on 12/11/15.
 */
class FormattedEntitySink(resource: WritableResource, formatter: EntityFormatter) extends EntitySink with TripleSink {

  private var properties = Seq[String]()

  // We optimize cases in which the resource is a file resource
  private val javaFile = resource match {
    case f: FileResource => Some(f.file)
    case _ => None
  }

  private var writer: Writer = null

  override def open(properties: Seq[String]) {
    this.properties = properties
    // If we got a java file, we write directly to it, otherwise we write to a temporary string
    writer = javaFile match {
      case Some(file) =>
        file.getParentFile.mkdirs()
        new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"))
      case None => new StringWriter()
    }
    //Write header
    writer.write(formatter.header)
  }

  override def writeEntity(subject: String, values: Seq[Seq[String]]) {
    for((property, valueSet) <- properties zip values;
        value <- valueSet) {
      writeStatement(subject, property, value)
    }
  }

  private def writeStatement(subject: String, predicate: String, value: String): Unit = {
    writer.write(formatter.formatLiteralStatement(subject, predicate, value))
  }

  override def close() {
    if (writer != null) {
      writer.write(formatter.footer)
      writer.close()
      // In case we used a string writer, we still need to write the generated string.
      writer match {
        case stringWriter: StringWriter => resource.write(stringWriter.toString)
        case _ =>
      }
      writer = null
    }
  }

  override def init(): Unit = {
    open(properties = Seq())
  }

  override def writeTriple(subject: String, predicate: String, value: String): Unit = {
    writeStatement(subject, predicate, value)
  }
}