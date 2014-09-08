package de.fuberlin.wiwiss.silk.plugins.dataset.xml

import de.fuberlin.wiwiss.silk.dataset.DataSource
import de.fuberlin.wiwiss.silk.entity._
import de.fuberlin.wiwiss.silk.runtime.resource.Resource

import scala.xml.{NodeSeq, XML}

class XmlSource(file: Resource, basePath: String) extends DataSource {

  override def retrievePaths(restriction: SparqlRestriction, depth: Int, limit: Option[Int]): Traversable[(Path, Double)] = {
   // At the moment we just generate paths from the first xml node that is found
   val xml = loadXmlNodes().head
   for(child <- xml.child) yield {
     (Path(restriction.variable, ForwardOperator("<" + child.label + ">") :: Nil), 1.0)
   }
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
          basePath.stripPrefix("/" + xml.label + "/")
        else
          basePath.stripPrefix("/")
      // Move to base path
      var node: NodeSeq = xml
      for(label <- path.split('/') if !label.isEmpty) {
        node = node \ label
      }
      node
    }
  }

  private class Entities(xml: NodeSeq, entityDesc: EntityDescription) extends Traversable[Entity] {
    def foreach[U](f: Entity => U) {
      // Enumerate entities
      for(node <- xml) {
         val values = for(path <- entityDesc.paths) yield evaluatePath(node, path)
         f(new Entity(node.label, values, entityDesc))
      }
    }

    private def evaluatePath(node: NodeSeq, path: Path): Set[String] = {
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
