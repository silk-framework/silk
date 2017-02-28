package org.silkframework.plugins.dataset.xml

import org.silkframework.entity._
import scala.xml.{Node, Text}

/**
  * Implementation of XML access functions.
  */
case class XmlTraverser(node: Node, parentOpt: Option[XmlTraverser] = None) {

  /**
    * All direct children of this node.
    */
  def children: Seq[XmlTraverser] = {
    for(child <- node \ "_") yield {
      XmlTraverser(child, Some(this))
    }
  }

  /**
    * All direct and indirect children of this node.
    */
  def childrenRecursive: Seq[XmlTraverser] = {
    children ++ children.flatMap(_.childrenRecursive)
  }

  /**
    * Lists all parent nodes.
    */
  def parents: List[Node] = {
    parentOpt match {
      case Some(traverser) =>
        traverser.node :: traverser.parents
      case None =>
        Nil
    }
  }

  /**
    * Generates a ID for the given XML node that is unique inside the document.
    */
  def nodeId: String = {
    // As we do not have access to the line number, we use a hashcode and hope that it doesn't clash
    XmlTraverser.nodeId(node)
  }

  /**
    * Collects all direct and indirect paths for this node.
    *
    * @param onlyLeafNodes Only return leaf nodes
    * @return Sequence of all found paths
    */
  def collectPaths(onlyLeafNodes: Boolean): Seq[Path] = {
    for(pathOperators <- collectPathsRecursive(onlyLeafNodes, prefix = Seq.empty)) yield {
      Path(pathOperators.tail.toList)
    }
  }.distinct

  /**
    * Recursively collects all direct and indirect paths for this node.
    * Initially called by [[XmlTraverser.collectPaths]]
    *
    * @param onlyLeafNodes Only return leaf nodes
    * @param prefix Path prefix to be prepended to all found paths
    * @return Sequence of all found paths
    */
  private def collectPathsRecursive(onlyLeafNodes: Boolean, prefix: Seq[PathOperator]): Seq[Seq[PathOperator]] = {
    // Generate a path from the xml node itself
    val path = prefix :+ ForwardOperator(node.label)
    // Generate paths for all children nodes
    val childPaths = children.flatMap(_.collectPathsRecursive(onlyLeafNodes, path))
    // Generate paths for all attributes
    val attributes = node.attributes.asAttrMap.keys.toSeq
    val attributesPaths = attributes.map(attribute => path :+ ForwardOperator("@" + attribute))

    if(!onlyLeafNodes)
      Seq(path) ++ attributesPaths ++ childPaths
    else if (childPaths.isEmpty)
      Seq(path) ++ attributesPaths
    else
      attributesPaths ++ childPaths
  }

  /**
    * Evaluates a Silk path on this node.
    *
    * @param path A path relative to the given XML node.
    * @return A sequence of nodes that are matching the path.
    */
  def evaluatePath(path: Path): Seq[XmlTraverser] = {
    evaluateOperators(path.operators)
  }

  /**
    * Evaluates a Silk path on this node and returns the node values as strings.
    *
    * @param path A path relative to the given XML node.
    * @return A sequence of nodes that are matching the path.
    */
  def evaluatePathAsString(path: Path): Seq[String] = {
    val xml = evaluatePath(path)
    xml.map(_.node.text)
  }

  private def evaluateOperators(ops: List[PathOperator]): Seq[XmlTraverser] = {
    ops match {
      case Nil =>
        Seq(this)
      case op :: opsTail =>
        evaluateOperator(op).flatMap(_.evaluateOperators(opsTail))
    }
  }

  private def evaluateOperator(op: PathOperator): Seq[XmlTraverser] = {
    op match {
      case op: ForwardOperator => evaluateForwardOperator(op)
      case op: PropertyFilter => evaluatePropertyFilter(op)
      case op: BackwardOperator => evaluateBackwardOperator(op)
      case _ => throw new UnsupportedOperationException("Unsupported path operator: " + op.getClass.getSimpleName)
    }
  }

  private def evaluateForwardOperator(op: ForwardOperator): Seq[XmlTraverser] = {
    op.property.uri match {
      case "#id" =>
        Seq(XmlTraverser(Text(nodeId), Some(this)))
      case "#tag" =>
        Seq(XmlTraverser(Text(node.label), Some(this)))
      case "*" =>
        children
      case "**" =>
        childrenRecursive
      case uri if uri.startsWith("@") =>
        for {
          attr <- node.attributes.find(_.key == uri.tail).toSeq
          child <- attr.value
        } yield {
          XmlTraverser(child, Some(this))
        }
      case uri =>
        for(child <- node \ uri) yield {
          XmlTraverser(child, Some(this))
        }
    }
  }

  private def evaluateBackwardOperator(op: BackwardOperator): Seq[XmlTraverser] = {
    parentOpt match {
      case Some(parent) =>
        Seq(parent)
      case None =>
        throw new RuntimeException("Cannot go backward from root XML element! Backward property: " + op.property.uri)
    }
  }

  private def evaluatePropertyFilter(op: PropertyFilter): Seq[XmlTraverser] = {
    node.find(n => op.evaluate("\"" + (n \ op.property.uri).text + "\"")) match {
      case Some(n) =>
        Seq(this)
      case None =>
        Seq.empty
    }
  }

}

object XmlTraverser {

  /**
    * Generates a ID for a given XML node that is unique inside the document.
    */
  private def nodeId(node: Node): String = {
    // As we do not have access to the line number, we use a hashcode and hope that it doesn't clash
    node.hashCode.toString.replace('-', '1')
  }

}
