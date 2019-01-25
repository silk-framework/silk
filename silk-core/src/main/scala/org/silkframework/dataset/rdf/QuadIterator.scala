package org.silkframework.dataset.rdf

import java.io.{File, FileWriter}

import org.apache.jena.riot.RDFDataMgr
import org.silkframework.entity.Entity

/**
  * Provides an Iterator interface for [[Quad]] containing serialization and [[Entity]] transformation
  */
trait QuadIterator extends Iterator[Quad] {

  /**
    * A close function, forward any close function of the underlying source of this iterator (such as QueryExecution or closeable StatementIterators) if needed
    * Call this once after finishing the consumption of Quads.
    */
  val close: () => Unit

  /**
    * function to indicate that the QuadIterator is or is not empty
    */
  val hasQuad: () => Boolean

  /**
    * Function for provisioning of the next Quad
    */
  val nextQuad: () => Quad

  /**
    * A formatter for serializing the Quad (or Triple) in a specific serialization
    */
  val formatter: QuadFormatter

  /**
    * Will generate an Entity for each Quad (using the EntitySchema of [[org.silkframework.execution.local.QuadEntityTable]]
    */
  def asEntities: Traversable[Entity]

  /**
    * Providing a [[TripleIterator]] by ignoring the context of each Quad
    */
  def asTriples: TripleIterator

  override def hasNext: Boolean = hasQuad()

  override def next(): Quad = nextQuad()

  /**
    * Will serialize the entire content of the Iterator using the defined formatter, thereby using it up and finally closing it
    */
  def serialize(): String = {
    val sb = new StringBuilder()
    sb.append(formatter.header)
    while(hasQuad()){
      sb.append(nextQuad().serialize(formatter))
      // line end
      sb.append("\n")
    }
    sb.append(formatter.footer)
    close()
    // to string
    sb.toString()
  }

  /**
    * Will serialize the content to the given file using the defined formatter, thereby using it up and finally closing it
    * NOTE: file will be overwritten
    * @param file - the file to write to
    */
  def saveToFile(file: File): Unit ={
    var writer: FileWriter = null
    try {
      file.createNewFile()
      writer = new FileWriter(file)
      writer.append(formatter.header)
      while (hasQuad()) {
        writer.append(nextQuad().serialize(formatter))
        // line end
        writer.append("\n")
      }
      writer.append(formatter.footer)
    }
    finally {
      close()
      writer.close()
    }
  }
}
