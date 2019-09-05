package org.silkframework.plugins.dataset.xml

import java.net.URLEncoder

import org.silkframework.dataset.DataSource
import org.silkframework.entity._
import org.silkframework.entity.paths._

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
    * Generates a URI for this node.
    */
  def generateUri(uriPattern: String): String = {
    if (uriPattern.isEmpty) {
      DataSource.generateEntityUri(node.label, nodeId)
    } else {
      XmlTraverser.uriRegex.replaceAllIn(uriPattern, m => {
        val pattern = m.group(1)
        val value = evaluatePath(UntypedPath.parse(pattern)).map(_.node.text).mkString("")
        URLEncoder.encode(value, "UTF8")
      })
    }
  }

  /**
    * Collects all direct and indirect paths for this node.
    *
    * @param onlyLeafNodes Only return leaf nodes
    * @return Sequence of all found paths
    */
  def collectPaths(onlyLeafNodes: Boolean, onlyInnerNodes: Boolean, depth: Int): Seq[TypedPath] = {
    assert(!(onlyInnerNodes && onlyLeafNodes), "onlyInnerNodes and onlyLeafNodes cannot be set to true at the same time")
    val ret = for(typedPath <- collectPathsRecursive(onlyLeafNodes, onlyInnerNodes, prefix = Seq.empty, depth) if typedPath.operators.size > 1) yield {
      TypedPath(UntypedPath(typedPath.operators.tail), typedPath.valueType, typedPath.isAttribute)
    }
    ret.distinct
  }

  /**
    * Recursively collects all direct and indirect paths for this node.
    * Initially called by [[XmlTraverser.collectPaths]]
    *
    * @param onlyLeafNodes  Only return leaf nodes
    * @param onlyInnerNodes Only return inner nodes
    * @param prefix         Path prefix to be prepended to all found paths
    * @return Sequence of all found paths
    */
  private def collectPathsRecursive(onlyLeafNodes: Boolean,
                                    onlyInnerNodes: Boolean,
                                    prefix: Seq[PathOperator],
                                    depth: Int): Seq[TypedPath] = {
    // Generate a path from the xml node itself
    val path = prefix :+ ForwardOperator(node.label)
    // Generate paths for all children nodes
    val childPaths = if(depth == 0) Seq() else children.flatMap(_.collectPathsRecursive(onlyLeafNodes, onlyInnerNodes, path, depth - 1))
    // Generate paths for all attributes
    val attributes = if(depth == 0) Seq() else node.attributes.asAttrMap.keys.toSeq
    val attributesPaths = attributes.map(attribute => TypedPath((path :+ ForwardOperator("@" + attribute)).toList, StringValueType, xmlAttribute = true))
    // Paths to inner nodes become object paths (URI), else value paths (string)
    val pathValueType: ValueType = if(children.nonEmpty || node.attributes.nonEmpty) UriValueType else StringValueType
    val typedPath = TypedPath(path.toList, pathValueType, xmlAttribute = false)

    if(onlyInnerNodes && children.isEmpty && node.attributes.isEmpty) {
      Seq() // An inner node has at least an attribute or child elements
    } else if(onlyInnerNodes) {
      Seq(typedPath) ++ childPaths
    } else if(!onlyLeafNodes) {
      Seq(typedPath) ++ attributesPaths ++ childPaths
    } else if (onlyLeafNodes && children.isEmpty) {
      Seq(typedPath) ++ attributesPaths // An XML element with a possible Text Element inside and attributes, but no child elements
    } else {
      attributesPaths ++ childPaths
    }
  }

  /**
    * Evaluates a Silk path on this node.
    *
    * @param path A path relative to the given XML node.
    * @return A sequence of nodes that are matching the path.
    */
  def evaluatePath(path: UntypedPath): Seq[XmlTraverser] = {
    evaluateOperators(path.operators)
  }

  /**
    * Evaluates a Silk path on this node and returns the node values as strings.
    *
    * @param path A path relative to the given XML node.
    * @return A sequence of nodes that are matching the path.
    */
  def evaluatePathAsString(path: TypedPath, uriPattern: String): Seq[String] = {
    val fetchEntityUri = path.valueType == UriValueType
    val xml = evaluatePath(path.toUntypedPath)
    xml.flatMap(_.formatNode(uriPattern, fetchEntityUri))
  }

  /**
    * Formats this node as String.
    * For leaf nodes, the text inside the node is returned.
    * For non-leaf nodes, a URI is generated.
    */
  private def formatNode(uriPattern: String, fetchEntityUri: Boolean): Option[String] = {
    // Check if this is a leaf node
    if(!fetchEntityUri && (node.isInstanceOf[Text] || (node.child.size == 1 && node.child.head.isInstanceOf[Text]))) {
      Some(node.text)
    } else if(uriPattern.nonEmpty || fetchEntityUri) {
      Some(generateUri(uriPattern))
    } else {
      None
    }
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
      case "#text" =>
        Seq(this)
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

  private val uriRegex = "\\{([^\\}]+)\\}".r

  /**
    * Generates a ID for a given XML node that is unique inside the document.
    */
  private def nodeId(node: Node): String = {
    // As we do not have access to the line number, we use a hashcode and hope that it doesn't clash
    node.hashCode.toString.replace('-', '1')
  }

}
