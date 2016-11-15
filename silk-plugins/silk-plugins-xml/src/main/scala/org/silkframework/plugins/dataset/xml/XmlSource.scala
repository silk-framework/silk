package org.silkframework.plugins.dataset.xml

import java.net.URLEncoder
import java.util.logging.{Level, Logger}

import org.silkframework.dataset.DataSource
import org.silkframework.entity._
import org.silkframework.runtime.resource.Resource
import org.silkframework.util.Uri

import scala.xml.{Node, NodeSeq, XML}

class XmlSource(file: Resource, basePath: String, uriPattern: String) extends DataSource {

  private val logger = Logger.getLogger(getClass.getName)

  private val uriRegex = "\\{([^\\}]+)\\}".r

  override def retrievePaths(t: Uri, depth: Int, limit: Option[Int]): IndexedSeq[Path] = {
    // At the moment we just generate paths from the first xml node that is found
    val xml = loadXmlNodes().head.node
    for (path <- XmlParser.collectPaths(xml).toIndexedSeq) yield {
      Path(path.tail.toList)
    }
  }

  override def retrieve(entitySchema: EntitySchema, limit: Option[Int] = None): Traversable[Entity] = {
    logger.log(Level.FINE, "Retrieving data from XML.")

    new Entities(loadXmlNodes(), entitySchema)
  }

  override def retrieveByUri(entitySchema: EntitySchema, entities: Seq[Uri]): Seq[Entity] = {
    throw new UnsupportedOperationException("Retrieving single entities from XML is currently not supported")
  }

  private case class XmlTraverser(parentOpt: Option[XmlTraverser], node: Node) {
    def parents: List[Node] = {
      parentOpt match {
        case Some(traverser) =>
          traverser.node :: traverser.parents
        case None =>
          Nil
      }
    }
  }

  /**
    * Returns the XML nodes found at the base path and
    *
    * @return
    */
  private def loadXmlNodes(): Seq[XmlTraverser] = {
    // Load XML
    val xml = XML.load(file.load)
    val rootTraverser = XmlTraverser(None, xml)
    // Resolve the base path
    if (basePath.isEmpty) {
      // If the base path is empty, we read all direct children of the root element
      (xml \ "_").map(n => XmlTraverser(Some(rootTraverser), n))
    } else {
      // As it may not be clear whether the base path must include the root element, we accept both
      val path =
        if (basePath.startsWith("/" + xml.label))
          basePath.stripPrefix("/" + xml.label)
        else
          basePath
      // Move to base path
      evaluateXPath(rootTraverser, path)
    }
  }

  private def evaluateXPath(traverser: XmlTraverser, path: String): Seq[XmlTraverser] = {
    val pathElements = path.stripPrefix("/").split('/').filterNot(_.isEmpty).toList
    evaluateXPathRec(traverser, pathElements)
  }

  private def evaluateXPathRec(traverser: XmlTraverser, pathElements: List[String]): Seq[XmlTraverser] = {
    pathElements match {
      case Nil =>
        Seq(traverser)
      case label :: pathTail =>
        val nextNodes = traverser.node \ label
        nextNodes.flatMap(n => evaluateXPathRec(XmlTraverser(Some(traverser), n), pathTail))
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
                index.toString
              } else {
                val traversers = evaluateXPath(traverser, pattern)
                val nodeSeq = NodeSeq.fromSeq(traversers.map(_.node))
                URLEncoder.encode(nodeSeq.text, "UTF8")
              }
            })
          }

        val values = for (path <- entityDesc.paths) yield evaluateSilkPath(traverser.node, path, traverser.parents)
        f(new Entity(uri, values, entityDesc))
      }
    }

    private def evaluateSilkPath(node: Node, path: Path, parentNodes: List[Node]): Seq[String] = {
      val xml = evaluateOperators(node, path.operators, parentNodes)
      xml.map(_.text)
    }

    private def evaluateOperators(node: Node, ops: List[PathOperator], parentNodes: List[Node]): NodeSeq = {
      ops match {
        case Nil =>
          node
        case op :: opsTail =>
          op match {
            case ForwardOperator(p) =>
              val forwardNodes =
                if(p.uri.startsWith("@")) {
                  val attr = node.attributes.find(_.key == p.uri.tail).get
                  attr.value
                } else {
                  node \ p.uri
                }
              forwardNodes flatMap { forwardNode =>
                evaluateOperators(forwardNode, opsTail, node :: parentNodes)
              }
            case p @ PropertyFilter(prop, cmp, value) =>
              node.filter(n => p.evaluate("\"" + (n \ prop.uri).text + "\"")).headOption match {
                case Some(n) =>
                  evaluateOperators(n, opsTail, parentNodes)
                case None =>
                  NodeSeq.Empty
              }
            case BackwardOperator(p) =>
              parentNodes match {
                case parent :: parentTail =>
                  evaluateOperators(parent, opsTail, parentTail)
                case Nil =>
                  throw new RuntimeException("Cannot go backward from root XML element! Backward property: " + p.uri)
              }
            case _ => throw new UnsupportedOperationException("Unsupported path operator: " + op.getClass.getSimpleName)
          }
      }
    }
  }

}
