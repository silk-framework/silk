package org.silkframework.dataset.rdf

import java.io.{File, FileWriter}

/**
  * Formats the content of an iterator.
  */
object IteratorFormatter {
  /**
    * Will serialize the entire content of the Iterator using the defined formatter, thereby using it up and finally closing it
    */
  def serialize[T](iterator: ClosableIterator[T], formatter: ElementFormatter[T]): String = {
    val sb = new StringBuilder()
    sb.append(formatter.header)
    while(iterator.hasNext){
      sb.append(formatter.formatElement(iterator.next()))
      // line end
      sb.append("\n")
    }
    sb.append(formatter.footer)
    iterator.close()
    // to string
    sb.toString()
  }

  /**
    * Will serialize the content to the given file using the defined formatter, thereby using it up and finally closing it
    * NOTE: file will be overwritten
    * @param file - the file to write to
    */
  def saveToFile[T](file: File, iterator: ClosableIterator[T], formatter: ElementFormatter[T]): Unit ={
    var writer: FileWriter = null
    try {
      file.createNewFile()
      writer = new FileWriter(file)
      writer.append(formatter.header)
      while (iterator.hasNext) {
        writer.append(formatter.formatElement(iterator.next()))
        // line end
        writer.append("\n")
      }
      writer.append(formatter.footer)
    }
    finally {
      iterator.close()
      writer.close()
    }
  }
}
