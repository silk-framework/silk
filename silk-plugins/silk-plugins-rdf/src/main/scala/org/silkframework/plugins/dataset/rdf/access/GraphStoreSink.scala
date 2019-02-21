package org.silkframework.plugins.dataset.rdf.access

import java.io.{BufferedOutputStream, File, FileOutputStream, OutputStream}
import java.nio.file.Files
import java.util.logging.Logger

import org.silkframework.dataset.rdf.{GraphStoreFileUploadTrait, GraphStoreTrait, Quad}
import org.silkframework.dataset._
import org.silkframework.entity.{Link, ValueType}
import org.silkframework.plugins.dataset.rdf.formatters.RdfFormatter
import org.silkframework.runtime.activity.UserContext
import org.silkframework.util.Uri

import scala.util.Try

/**
  * An RDF sink based on the graph store protocol.
  *
  * @see https://www.w3.org/TR/sparql11-http-rdf-update/
  */
case class GraphStoreSink(graphStore: GraphStoreTrait,
                          graphUri: String,
                          formatterOpt: Option[RdfFormatter],
                          comment: Option[String],
                          dropGraphOnClear: Boolean) extends EntitySink with LinkSink with TripleSink with RdfSink {

  private var properties = Seq[TypedProperty]()
  private var output: Option[OutputStream] = None
  private val log = Logger.getLogger(classOf[GraphStoreSink].getName)
  private var stmtCount = 0
  private var byteCount = 0L
  // If a file upload graph store is used this will buffer the intermediate result that will be added via file upload.
  private var tempFile: Option[File] = None
  private val maxBytesPerRequest = graphStore.defaultTimeouts.maxRequestSize // in bytes

  override def openTable(typeUri: Uri, properties: Seq[TypedProperty])(implicit userContext: UserContext): Unit = {
    init()
    this.properties = properties
  }

  override def writeEntity(subject: String, values: Seq[Seq[String]])
                          (implicit userContext: UserContext): Unit = {
    for((property, valueSet) <- properties zip values; value <- valueSet) {
      if(property.isBackwardProperty) {
        writeStatement(value, property.propertyUri, subject, property.valueType)
      } else {
        writeStatement(subject, property.propertyUri, value, property.valueType)
      }
    }
  }

  override def writeLink(link: Link, predicateUri: String)
                        (implicit userContext: UserContext): Unit = {
    val (newStatements, _) = formatLink(link, predicateUri)
    writeStatementString(newStatements)
  }

  override def init()(implicit userContext: UserContext): Unit = {
    if(output.isEmpty) {
      stmtCount = 0
      byteCount = 0L
      output = initOutputStream
      log.fine("Initialized graph store sink.")
    }
  }

  private def initOutputStream(implicit userContext: UserContext): Option[OutputStream] = {
    graphStore match {
      case _: GraphStoreFileUploadTrait =>
        val file = Files.createTempFile("graphStoreUpload", ".nt").toFile
        file.deleteOnExit()
        tempFile = Some(file)
        log.fine("Created temporary file for graphstore file upload.")
        Some(new BufferedOutputStream(new FileOutputStream(file)))
      case _: GraphStoreTrait =>
        // Always use N-Triples because of stream-ability
        val out = graphStore.postDataToGraph(graphUri, comment = comment)
        Some(out)
    }
  }

  override def writeStatement(subject: String, property: String, value: String, valueType: ValueType)
                             (implicit userContext: UserContext): Unit = {
    val stmtString: String = buildStatementString(subject, property, value, valueType)
    writeStatementString(stmtString)
    stmtCount += 1
  }

  // Writes an N-Triples statement to the output stream.
  private def writeStatementString(stmtString: String)
                                  (implicit userContext: UserContext): Unit = {
    output match {
      case Some(o) =>
        val outBytes = stmtString.getBytes("UTF-8")
        val outputLength = outBytes.length
        if(byteCount + outputLength > maxBytesPerRequest) {
          log.fine("Reached max bytes per request size limit, ending and starting new connection.")
          close()
          init()
        }
        byteCount += outputLength
        o.write(outBytes)
      case None =>
        throw new IllegalStateException("Writing to a closed Graph Store output stream!")
    }
  }

  override def writeTriple(subject: String, predicate: String, obj: String, valueType: ValueType)
                          (implicit userContext: UserContext): Unit = {
    writeStatement(subject, predicate, obj, valueType)
  }

  override def clear()(implicit userContext: UserContext): Unit = {
    if(dropGraphOnClear) {
      log.fine("Clearing graph " + graphUri)
      graphStore.deleteGraph(graphUri)
    }
  }

  override def closeTable()(implicit userContext: UserContext): Unit = {}

  override def close()(implicit userContext: UserContext): Unit = {
    output match {
      case Some(o) =>
        try {
          o.close()
          graphStore match {
            case fileUploadGraphStore: GraphStoreFileUploadTrait =>
              tempFile match {
                case Some(fileToUpload) =>
                  fileUploadGraphStore.uploadFileToGraph(graphUri, fileToUpload, "application/n-triples", comment)
                case None =>
                  throw new IllegalStateException("GraphStore file upload error: No temporary file exists even though an output stream exists!")
              }
            case _: GraphStoreTrait =>
          }
        } finally {
          tempFile foreach { file => Try(file.delete()) }
          output = None
        }
      case None =>
        // no effect
    }
  }
}
