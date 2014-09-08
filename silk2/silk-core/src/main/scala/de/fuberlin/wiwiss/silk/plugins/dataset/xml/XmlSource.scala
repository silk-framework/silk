package de.fuberlin.wiwiss.silk.plugins.dataset.xml

import java.net.URLEncoder

import de.fuberlin.wiwiss.silk.dataset.DataSource
import de.fuberlin.wiwiss.silk.entity._
import de.fuberlin.wiwiss.silk.runtime.resource.Resource

import scala.xml.{Node, NodeSeq, XML}

class XmlSource(file: Resource, basePath: String, uriPrefix: String, idPath: String) extends DataSource {

  override def retrievePaths(restriction: SparqlRestriction, depth: Int, limit: Option[Int]): Traversable[(Path, Double)] = {
   // At the moment we just generate paths from the first xml node that is found
   val xml = loadXmlNodes().head
   for(path <- collectPaths(Nil, xml)) yield {
     (Path(restriction.variable, path.tail.toList), 1.0)
   }
  }

  /**
   * Collects all direct and indirect paths from an xml node
   * @param prefix Path prefix to be prepended to all found paths
   * @param node The xml node to search paths in
   * @return Sequence of all found paths
   */
  private def collectPaths(prefix: Seq[PathOperator], node: Node): Seq[Seq[PathOperator]] = {
    // Generate a path from the xml node itself
    val path = prefix :+ ForwardOperator(node.label)
    // Generate paths for all children nodes
    val childNodes = node \ "_"
    val childPaths = childNodes.flatMap(child => collectPaths(path, child))
    // We only want to generate paths for leave nodes
    if(childPaths.isEmpty) Seq(path) else childPaths
  }

  override def retrieve(entityDesc: EntityDescription, entities: Seq[String] = Seq.empty): Traversable[Entity] = {
    new Entities(loadXmlNodes(), entityDesc)
  }

  private def loadXmlNodes() = {
    // Load XML
    val xml = XML.load(file.load)
    // Resolve the base path
    if(basePath.isEmpty) {
      // If the base path is empty, we read all direct children of the root element
      xml \ "_"
    } else {
      // As it may not be clear whether the base path must include the root element, we accept both
      val path =
        if(basePath.startsWith("/" + xml.label))
          basePath.stripPrefix("/" + xml.label)
        else
          basePath
      // Move to base path
      evaluateXPath(xml, path)
    }
  }

  private def evaluateXPath(node: Node, path: String): NodeSeq = {
    var currentNode: NodeSeq = node
    for(label <- path.stripPrefix("/").split('/') if !label.isEmpty) {
      currentNode = currentNode \ label
    }
    currentNode
  }

  private class Entities(xml: NodeSeq, entityDesc: EntityDescription) extends Traversable[Entity] {
    def foreach[U](f: Entity => U) {
      // Enumerate entities
      for((node, index) <- xml.zipWithIndex) {
         val uri =
           if(idPath.isEmpty)
             uriPrefix + node.label + index
           else
             uriPrefix + URLEncoder.encode(evaluateXPath(node, idPath).text, "UTF8")
         val values = for(path <- entityDesc.paths) yield evaluateSilkPath(node, path)
         f(new Entity(uri, values, entityDesc))
      }
    }

    private def evaluateSilkPath(node: NodeSeq, path: Path): Set[String] = {
      var xml = node
      for(op <- path.operators) {
        xml = evaluateOperator(xml, op)
      }
      xml.map(_.text).toSet
    }

    private def evaluateOperator(node: NodeSeq, op: PathOperator): NodeSeq = op match {
      case ForwardOperator(p) => node \ p.uri
      case _ => throw new UnsupportedOperationException("Unsupported path operator: " + op.getClass.getSimpleName)
    }
  }
}
