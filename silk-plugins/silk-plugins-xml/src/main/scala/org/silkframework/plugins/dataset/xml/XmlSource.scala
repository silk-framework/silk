package org.silkframework.plugins.dataset.xml

import java.net.URLEncoder
import java.util.logging.{Level, Logger}

import org.silkframework.config.Prefixes
import org.silkframework.dataset.DataSource
import org.silkframework.entity._
import org.silkframework.runtime.resource.Resource
import org.silkframework.util.Uri

import scala.xml.XML

class XmlSource(file: Resource, basePath: String, uriPattern: String) extends DataSource {

  private val logger = Logger.getLogger(getClass.getName)

  private val uriRegex = "\\{([^\\}]+)\\}".r

  override def retrieveTypes(limit: Option[Int]): Traversable[(String, Double)] = {
    // At the moment we just generate paths from the first xml node that is found
    val xml = XML.load(file.load)
    for(pathOperators <- XmlTraverser(xml).collectPaths()) yield {
      (Path(pathOperators.toList).serialize(Prefixes.empty), 1.0 / pathOperators.size)
    }
  }

  override def retrievePaths(t: Uri, depth: Int, limit: Option[Int]): IndexedSeq[Path] = {
    // At the moment we just generate paths from the first xml node that is found
    val xml = loadXmlNodes(t.uri).head
    for (path <- xml.collectPaths().toIndexedSeq) yield {
      Path(path.tail.toList)
    }
  }

  override def retrieve(entitySchema: EntitySchema, limit: Option[Int] = None): Traversable[Entity] = {
    logger.log(Level.FINE, "Retrieving data from XML.")

    val nodes = loadXmlNodes(entitySchema.typeUri.uri)
    new Entities(nodes, entitySchema)
  }

  override def retrieveByUri(entitySchema: EntitySchema, entities: Seq[Uri]): Seq[Entity] = {
    throw new UnsupportedOperationException("Retrieving single entities from XML is currently not supported")
  }

  /**
    * Returns the XML nodes found at the base path and
    *
    * @return
    */
  private def loadXmlNodes(typeUri: String): Seq[XmlTraverser] = {
    // If a type URI is provided, we use it as path. Otherwise we are using the base Path (which is deprecated)
    val pathStr = if(typeUri.isEmpty) basePath else typeUri
    // Load XML
    val xml = XML.load(file.load)
    val rootTraverser = XmlTraverser(xml)
    // Resolve the base path
    if (pathStr.isEmpty) {
      // If the base path is empty, we read all direct children of the root element
      rootTraverser.children
    } else {
      // As it may not be clear whether the base path must include the root element, we accept both
      val path =
        if (pathStr.startsWith("/" + xml.label))
          pathStr.stripPrefix("/" + xml.label)
        else
          pathStr
      // Move to base path
     rootTraverser.evaluatePath(Path.parse(path))
    }
  }

  private class Entities(xml: Seq[XmlTraverser], entityDesc: EntitySchema) extends Traversable[Entity] {
    def foreach[U](f: Entity => U) {
      // Enumerate entities
      for ((traverser, index) <- xml.zipWithIndex) {
        val uri =
          if (uriPattern.isEmpty) {
            traverser.node.label + index
          } else {
            uriRegex.replaceAllIn(uriPattern, m => {
              val pattern = m.group(1)
              if (pattern == "#") {
                traverser.nodeId
              } else {
                val value = traverser.evaluatePathAsString(Path.parse(pattern)).mkString("")
                URLEncoder.encode(value, "UTF8")
              }
            })
          }

        val values = for (typedPath <- entityDesc.typedPaths) yield traverser.evaluatePathAsString(typedPath.path)
        f(new Entity(uri, values, entityDesc))
      }
    }
  }

}
