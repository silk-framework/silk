package org.silkframework.plugins.dataset.text

import org.silkframework.config.Prefixes
import org.silkframework.dataset.{EntitySink, LinkSink, TypedProperty}
import org.silkframework.entity.Link
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.Uri

import java.io.{OutputStreamWriter, Writer}

class TextFileSink(ds: TextFileDataset) extends EntitySink with LinkSink {

  private var writer: Option[Writer] = None

  override def init()(implicit userContext: UserContext, prefixes: Prefixes): Unit = {
    if(writer.isEmpty) {
      writer = Some(new OutputStreamWriter(ds.bulkWritableResource.createOutputStream(), ds.charset))
    }
  }

  override def openTable(typeUri: Uri, properties: Seq[TypedProperty], singleEntity: Boolean = false)(implicit userContext: UserContext, prefixes: Prefixes): Unit = {
    init()
  }

  override def closeTable()(implicit userContext: UserContext): Unit = { }

  override def writeEntity(subject: String, values: IndexedSeq[Seq[String]])(implicit userContext: UserContext): Unit = {
    writer match {
      case Some(w) =>
        w.write(values.flatten.mkString("", " ", "\n"))
      case None =>
        throw new ValidationException("TextFileSink must be opened first")
    }

  }

  override def writeLink(link: Link, predicateUri: String, inversePredicateUri: Option[String])
                        (implicit userContext: UserContext, prefixes: Prefixes): Unit = {
    writeEntity("", IndexedSeq(Seq(link.source), Seq(link.target)))
  }

  override def clear(force: Boolean = false)(implicit userContext: UserContext): Unit = {
    ds.bulkWritableResource.writeString("", codec = ds.codec)
  }

  override def close()(implicit userContext: UserContext): Unit = {
    try {
      writer.foreach(_.close())
    } finally {
      writer = None
    }
  }
}
