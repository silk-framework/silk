package org.silkframework.plugins.dataset.xml

import java.util.logging.{Level, Logger}

import org.silkframework.config.{DefaultConfig, PlainTask, Task}
import org.silkframework.dataset._
import org.silkframework.entity._
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.Resource
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.{Identifier, Uri}

import scala.xml.XML

class XmlSourceInMemory(file: Resource, basePath: String, uriPattern: String) extends DataSource
    with PathCoverageDataSource with ValueCoverageDataSource with PeakDataSource with XmlSourceTrait with HierarchicalSampleValueAnalyzerExtractionSource {

  private val logger = Logger.getLogger(getClass.getName)

  override def retrieveTypes(limit: Option[Int])
                            (implicit userContext: UserContext): Traversable[(String, Double)] = {
    val xml = file.read(XML.load)
    for (path <- Path.empty +: XmlTraverser(xml).collectPaths(onlyLeafNodes = false, onlyInnerNodes = true, depth = Int.MaxValue).map(_.toSimplePath)) yield {
      (path.normalizedSerialization, 1.0 / path.operators.size)
    }
  }

  override def retrievePaths(typeUri: Uri, depth: Int = Int.MaxValue, limit: Option[Int] = None)
                            (implicit userContext: UserContext): IndexedSeq[TypedPath] = {
    retrieveXmlPaths(typeUri, depth, limit, onlyLeafNodes = false, onlyInnerNodes = false)
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
                       (implicit userContext: UserContext): Traversable[Entity] = {
    logger.log(Level.FINE, "Retrieving data from XML.")

    val nodes = loadXmlNodes(entitySchema.typeUri.uri)
    val subTypeEntities = if(entitySchema.subPath.operators.nonEmpty) {
      nodes.flatMap(_.evaluatePath(entitySchema.subPath))
    } else { nodes }
    val entities = new Entities(subTypeEntities, entitySchema)

    limit match {
      case Some(max) => entities.take(max)
      case None => entities
    }
  }

  /**
    * Returns the XML nodes found at the base path and
    *
    * @return
    */
  private def loadXmlNodes(typeUri: String): Seq[XmlTraverser] = {
    // If a type URI is provided, we use it as path. Otherwise we are using the base Path (which is deprecated)
    val pathStr = if (typeUri.isEmpty) basePath else typeUri
    // Load XML
    val xml = file.read(XML.load)
    val rootTraverser = XmlTraverser(xml)
    // Move to base path
    rootTraverser.evaluatePath(Path.parse(pathStr))
  }

  private class Entities(xml: Seq[XmlTraverser], entityDesc: EntitySchema) extends Traversable[Entity] {
    def foreach[U](f: Entity => U) {
      // Enumerate entities
      for ((traverser, index) <- xml.zipWithIndex) {
        val uri = traverser.generateUri(uriPattern)
        val values = for (typedPath <- entityDesc.typedPaths) yield traverser.evaluatePathAsString(typedPath, uriPattern)
        f(Entity(uri, values, entityDesc))
      }
    }
  }

  override def combinedPath(typeUri: String, inputPath: Path): Path = {
    val typePath = Path.parse(typeUri)
    Path(typePath.operators ++ inputPath.operators)
  }

  override def convertToIdPath(path: Path): Option[Path] = {
    Some(Path(path.operators ::: List(ForwardOperator("#id"))))
  }

  override def peak(entitySchema: EntitySchema, limit: Int)
                   (implicit userContext: UserContext): Traversable[Entity] = {
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
  override def underlyingTask: Task[DatasetSpec[Dataset]] = PlainTask(Identifier.fromAllowed(file.name), DatasetSpec(EmptyDataset))   //FIXME CMEM-1352 replace with actual task
}


