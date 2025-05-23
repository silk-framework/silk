package org.silkframework.plugins.dataset.xml

import org.silkframework.config.{PlainTask, Prefixes, Task}
import org.silkframework.dataset._
import org.silkframework.entity._
import org.silkframework.entity.paths.{ForwardOperator, TypedPath, UntypedPath}
import org.silkframework.execution.EntityHolder
import org.silkframework.execution.local.GenericEntityTable
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.iterator.CloseableIterator
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.resource.Resource
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.{Identifier, Uri}

import java.util.logging.{Level, Logger}

class XmlSourceInMemory(file: Resource, basePath: String, uriPattern: String) extends DataSource
    with PathCoverageDataSource with ValueCoverageDataSource with PeakDataSource with XmlSourceTrait with HierarchicalSampleValueAnalyzerExtractionSource {

  private val logger = Logger.getLogger(getClass.getName)

  override val supportsAsteriskOperator: Boolean = true

  override def retrieveTypes(limit: Option[Int])
                            (implicit userContext: UserContext, prefixes: Prefixes): Iterable[(String, Double)] = {
    new XmlSourceStreaming(file, basePath, uriPattern).retrieveTypes(limit)
  }

  override def retrievePaths(typeUri: Uri, depth: Int = Int.MaxValue, limit: Option[Int] = None)
                            (implicit userContext: UserContext, prefixes: Prefixes): IndexedSeq[TypedPath] = {
    new XmlSourceStreaming(file, basePath, uriPattern).retrievePaths(typeUri, depth, limit)
  }

  override def retrieveXmlPaths(typeUri: Uri, depth: Int, limit: Option[Int], onlyLeafNodes: Boolean, onlyInnerNodes: Boolean): IndexedSeq[TypedPath] = {
    // At the moment we just generate paths from the first xml node that is found
    val xml = loadXmlNodes(typeUri.uri)
    if (xml.isEmpty) {
      throw new ValidationException(s"There are no XML nodes at the given path ${typeUri.toString} in resource ${file.name}")
    } else {
      xml.head.collectPaths(onlyLeafNodes = onlyLeafNodes, onlyInnerNodes = onlyInnerNodes, depth).toIndexedSeq
    }
  }

  override def retrieve(entitySchema: EntitySchema, limit: Option[Int] = None)
                       (implicit context: PluginContext): EntityHolder = {
    logger.log(Level.FINE, "Retrieving data from XML.")

    val entities = retrieveEntities(entitySchema)

    val limitedEntities =
      limit match {
        case Some(max) => entities.take(max)
        case None => entities
     }

    GenericEntityTable(limitedEntities, entitySchema, underlyingTask)
  }

  private case class XMLReadException(msg: String, cause: Throwable) extends RuntimeException(msg, cause)

  /**
    * Returns the XML nodes found at the base path and
    *
    * @return
    */
  private def loadXmlNodes(typeUri: String): Seq[XmlTraverser] = {
    // Load XML
    val xml = new XmlSourceStreaming(file, basePath, uriPattern)
    val rootTraverser = XmlTraverser(xml.buildNode())

    // Move to base path
    val typeUriPart = if (typeUri.isEmpty) {
      ""
    } else if (typeUri.startsWith("\\") || typeUri.startsWith("/") || typeUri.startsWith("[")) {
      typeUri
    } else {
      "/" + typeUri
    }
    val pathStr = basePath + typeUriPart
    rootTraverser.evaluatePath(UntypedPath.parse(pathStr))
  }

  private def retrieveEntities(entitySchema: EntitySchema): Seq[Entity] = {
    // Load xml
    logger.fine("Loading XML")
    val nodes = loadXmlNodes(entitySchema.typeUri.uri)
    val xml = if(entitySchema.subPath.operators.nonEmpty) {
      nodes.flatMap(_.evaluatePath(entitySchema.subPath))
    } else { nodes }

    // Enumerate entities
    logger.fine("Reading XML")
    for (traverser <- xml) yield {
      val uri = traverser.generateUri(uriPattern)
      val values = for (typedPath <- entitySchema.typedPaths) yield traverser.evaluatePathAsString(typedPath, uriPattern)
      Entity(uri, values, entitySchema)
    }
  }

  override def combinedPath(typeUri: String, inputPath: UntypedPath): UntypedPath = {
    val typePath = UntypedPath.parse(typeUri)
    UntypedPath(typePath.operators ++ inputPath.operators)
  }

  override def convertToIdPath(path: UntypedPath): Option[UntypedPath] = {
    Some(UntypedPath(path.operators ::: List(ForwardOperator("#id"))))
  }

  override def peak(entitySchema: EntitySchema, limit: Int)
                   (implicit context: PluginContext): CloseableIterator[Entity] = {
    peakWithMaximumFileSize(file, entitySchema, limit)
  }

  override def collectPaths(limit: Int, collectValues: (List[String], String) => Unit): Seq[List[String]] = {
    // Re-use implementation of streaming based XML source
    new XmlSourceStreaming(file, basePath, uriPattern).collectPaths(limit, collectValues)
  }

  /**
    * The dataset task underlying the Datset this source belongs to
    *
    * @return
    */
  override lazy val underlyingTask: Task[DatasetSpec[Dataset]] = PlainTask(Identifier.fromAllowed(file.name), DatasetSpec(EmptyDataset))   //FIXME CMEM-1352 replace with actual task
}


