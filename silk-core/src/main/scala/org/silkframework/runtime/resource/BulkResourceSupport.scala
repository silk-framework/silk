package org.silkframework.runtime.resource

import java.io._
import java.util.logging.Logger
import java.util.zip.ZipException

import org.apache.commons.io.input.ReaderInputStream
import org.silkframework.entity.EntitySchema
import org.silkframework.runtime.resource.BulkResourceSupport.getDistinctSchemaDescriptions

import scala.collection.JavaConverters

/**
  * Trait for Datasets that need to support zipped files with multiple resources.
  * Provides a checkResource function that returns a Resource or a BulkResource, depending on the input.
  * See @BulkResource.
  * To fully support zipped or partitioned resources the implementing class should use the methods of
  * BulkResource. It provides a input stream on the concatenated content of the zip or a set of input streams
  * that can be used in more complex methods of combining the input.
  */
trait BulkResourceSupport {

  val log: Logger = Logger.getLogger(this.getClass.getSimpleName)

  /**
    * Returns a BulkResource depending on the given inputs location and name.
    * A BulkResource is returned if the file belonging to the given resource ends with .zip or is a
    * directory.
    *
    * @param resource WritableResource tha may be zip or folder
    * @return instance of BulkResource
    */
  def asBulkResource(resource: WritableResource): BulkResource = {
    if (resource.name.endsWith(".zip") && !new File(resource.path).isDirectory) {
      log info "Zip file Resource found."
      BulkResource(new File(resource.path))
    }
    else if (new File(resource.path).isDirectory) {
      log info "Resource Folder found."
      BulkResource(new File(resource.path))
    }
    else {
      throw new IllegalArgumentException(resource.path + " is not a bulk resource.")
    }
  }

  /**
    * Cast the iput and return a BulkResource object if the given WritableResource is a zip file.
    * Otherwise the given object is returned as is.
    *
    * Uses the dataset specific checkResourceSchema, onSingleSchemaBulkContent and onMultiSchemaBulkContent
    * function's to check the resource content and schema.
    *
    * @param file Resource
    * @return BulkResource or the given resource if it is no ar
    */
  def checkIfBulkResource(file: WritableResource): WritableResource = {
    if (isBulkResource(file)) {
      val bulkResource = asBulkResource(file)
      val schemaSet = getDistinctSchemaDescriptions(checkResourceSchema(bulkResource))

      if (schemaSet.isEmpty) {
        throw new Exception("The schema of the bulk resource could not be determined")
      }
      else if (schemaSet.length == 1) {
        log info s"One schema found for all resources in: ${bulkResource.name}"
        onSingleSchemaBulkContent(bulkResource).get
      }
      else {
        log info s"Multiple schemata found in: ${bulkResource.name}"
        onMultiSchemaBulkContent(bulkResource)
          .getOrElse(onSingleSchemaBulkContent(bulkResource).get)
      }
    }
    else {
      file
    }
  }


  /**
    * Returns true if the given resource is a BulkResource and false otherwise.
    * A BulkResource is detected if the file belonging to the given resource ends with .zip or is a
    * directory.
    *
    * @param resource WritableResource to check
    * @return true if an archive or folder
    */
  def isBulkResource(resource: WritableResource): Boolean = {
    resource.name.endsWith(".zip") && !new File(resource.path).isDirectory
  }


  /* The following methods that need to implement by the datasets to avoid dependency/structure changes
     In general it would be better to have a model with datasets that don't implement logic (like other operators).
     Then we could define arbitrary "flag" traits and leave the impl. to each executor.
     Now spark does the second thing and the local exec. does the first. However, the local way is unlikely to change.

     FIXME Rethink traits/execution model */

  /**
    * Gets called when it is detected that all files in the bulk resource have the different schemata.
    * The implementing class needs to provide a bulk resource object with an input stream that
    * covers all files.
    * If that case cannot be supported None should be returned.
    *
    * @param bulkResource Bulk resource
    * @return
    */
  def onMultiSchemaBulkContent(bulkResource: BulkResource): Option[BulkResource]

  /**
    * Gets called when it is detected that all files in the bulk resource have the same schema.
    * The implementing class needs to provide a logical concatenation of the individual resources.
    * If that case cannot be supported None should be returned.
    *
    * @param bulkResource Bulk resource
    * @return
    */
  def onSingleSchemaBulkContent(bulkResource: BulkResource): Option[BulkResource]

  /**
    * The implementing dataset must provide a way to determine the schema of each resource in the bulk resource.
    * The cardinality of the result is 1, there is only one schema.
    *
    * @param bulkResource Bulk resource
    * @return
    */
  def checkResourceSchema(bulkResource: BulkResource): Seq[EntitySchema]

}

/**
  * Companion object with helper functions.
  */
object BulkResourceSupport {

  /*constants*/
  final val GENERATED_XML_ROOT_NAMWE: String = "GENERATED_ROOT" // TODO make configurable? at leat check ex. root

  /*logger*/
  private val log: Logger = Logger.getLogger(this.getClass.getSimpleName)

  /**
    * Get a Seq of InputStream object, each belonging to on file in the given achieve.
    *
    * @param bulkResource Zip or resource folder
    * @return Sequence of InputStream objects
    */
  def getInputStreamSet(bulkResource: BulkResource): Seq[InputStream] = {
    try {
      bulkResource.inputStreams
    }
    catch {
      case t: Throwable =>
        log severe s"Exception for zip resource ${bulkResource.path}: " + t.getMessage
        throw new ZipException(t.getMessage)
    }
  }

  /**
    * Returns the input streams belonging to the input resource. One for each file in the zipped bulk resource.
    *
    * @param bulkResource Input resource
    * @return Set of Streams
    */
  def getIndividualStreams(bulkResource: BulkResource): Seq[InputStream] = {
    bulkResource.inputStreams
  }


  /**
    * Returns one input stream belonging to the input resource. This input stream logically is equal to the input stream
    * on the concatenation of the individual resources in the bulk resource.
    *
    * If skipLines is non empty the concatenated input stream will skip the provided amount of lines in each file except
    * the first.
    *
    * @param bulkResource Input resource
    * @param skipLines Lines to skip at the beginning of each file except the 1st
    * @return
    */
  def getConcatenatedStream(bulkResource: BulkResource, skipLines: Option[Int] = None): InputStream = {
     combineStreams(bulkResource.inputStreams, skipLines)
  }

  /**
    * Combines a sequence of input streams (hopefully without crossing the streams) into one logical concatenation.
    *
    * @param streams Sequence of InputStream objects
    * @return InputStream
    */
  def combineStreams(streams: Seq[InputStream], skipLines: Option[Int] = None): InputStream = {
    if (skipLines.isEmpty) {
      val streamEnumeration = JavaConverters.asJavaEnumerationConverter[InputStream](streams.iterator)
      val combi = new SequenceInputStream(streamEnumeration.asJavaEnumeration)
      val copy = copyStream(combi)
      combi.close()
      copy.reset()
      copy
    }
    else {
      if (skipLines.get > 0) {
        val head = streams.head
        val tail = streams.tail
        val newTail: Seq[InputStream] = tail.map(is => {
          val correctedStream = getSkipLinesInputStream(is, skipLines.get)
          new ReaderInputStream(new InputStreamReader(correctedStream))
        })
        combineStreams(Seq(head) ++ newTail, None)
      }
      else {
        throw new IllegalArgumentException("The line to skip must be None or a number greater 0.")
      }
    }
  }

  /**
    * Return a pair of input streams each containing a part of an xml tag ("<elementName>" and resp.
    * </elemName>") for combinations writh other treams in the combinesStreams function.
    *
    * @param elementName XM Element name, defaults to "GENERATED_ROOT"
    * @return
    */
  def getXmlElementWrapperInputStreams(elementName: String): (InputStream, InputStream) =
    (new ByteArrayInputStream(s"<$elementName>".getBytes()), new ByteArrayInputStream(s"</$elementName>".getBytes()))


  /**
    * Return an input stream that is equa to the given stream with the given
    * number of lines skipped.
    *
    * @param inputStream - input stream wirh lines to skip
    * @param linesToSkip - number of lines to skip
    * @return
    */
  private def getSkipLinesInputStream(inputStream: InputStream, linesToSkip: Int = 0): InputStream = {
    val lis = new LineNumberReader(new InputStreamReader(inputStream))
    for (i <-0 until linesToSkip) {
      val line = lis.readLine()
      log info s"Skipping line: $line in bulk resource."
    }
    combineStreams(Seq(getNewlineInputStream, new ReaderInputStream(lis)))
  }


  /**
    * Get an InputStream representing only a newline char.
    *
    * @return InputStream
    */
  def getNewlineInputStream: InputStream = {
    val newline = System.lineSeparator()
    new ByteArrayInputStream(newline.getBytes())
  }


  /**
    * Get only the schemata with different paths/types.
    */
  def getDistinctSchemaDescriptions(schemaSequence: Seq[EntitySchema]): Seq[EntitySchema] = {
    if (schemaSequence.isEmpty) {
      Seq.empty[EntitySchema]
    }
    else {
      schemaSequence.distinct
    } // should work since equals is overwritte and should apply here as well, although it looks like WIP TODO check it!
  }

  /**
    * Closes the given set of streams. Calls wait() before closing if that is possible.
    *
    * @param streams Sequence of InoutStreams
    */
  def closeStreamSet(streams: Seq[InputStream]): Unit = {
    if (streams.nonEmpty) streams.foreach( s => {
      try s.wait()
      catch {
        case t: Throwable =>
          log severe "Some kind of exception occured when closing an input stream: " + t.getMessage
      }
      finally s.close()
    })
  }


  /**
    * Closes the given niput stream. Calls wait() before closing if that is possible.
    *
    * @param stream Input stream
    */
  def closeStream(stream: InputStream): Unit = {
    if (stream != null) {
      try stream.wait()
      catch {
        case _: Throwable => // who cares
      }
      finally stream.close()
    }
  }


  /**
    * Create a resource with the meta data of the given bulk resource and the givin input stream as its backing
    * input stream.
    *
    * @param bulkResource Resource
    * @param unifiedInputStream The input stream that will be provided by the new resource
    * @return
    */
  def asWritableResource(bulkResource: BulkResource, unifiedInputStream: InputStream): Resource = {
    ReadOnlyResource(BulkResource.createFromBulkResource(bulkResource, unifiedInputStream))
  }

  /**
    * Copy an input stream.
    *
    * @param inputStream
    * @return
    */
  def copyStream(inputStream: InputStream): InputStream = {
  //  val text = scala.io.Source.fromInputStream(inputStream).mkString
    val stream: InputStream = new ByteArrayInputStream(streamToBytes(inputStream))
    inputStream.close()
    stream.reset()
    stream
  }


  def streamToBytes(inputStream: InputStream): Array[Byte] = {
    val len = 16384
    val buf = Array.ofDim[Byte](len)
    val out = new ByteArrayOutputStream

    @scala.annotation.tailrec
    def copy(): Array[Byte] = {
      val n = inputStream.read(buf, 0, len)
      if (n != -1) {
        out.write(buf, 0, n); copy()
      }
      else {
        out.toByteArray
      }
    }

    copy()
  }

  def printStream(inputStream: InputStream): Unit = {
    val copy = copyStream(inputStream)
    var txt = ""
    var b = 0
    b = copy.read()
    while (b > 0) {
      txt += b.toChar
      b = copy.read()

    }
    println(txt.replaceAll("\r",""))
  }

}
