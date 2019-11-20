package org.silkframework.plugins.dataset.xml

import java.net.URLEncoder

import org.silkframework.dataset.DataSource
import org.silkframework.entity._
import org.silkframework.entity.paths._

import scala.collection.mutable.ArrayBuffer
import scala.xml.Node

/**
  * Implementation of XML access functions.
  */
case class XmlTraverser(node: InMemoryXmlNode, parentOpt: Option[XmlTraverser] = None, uniqueFileId: String) {
  /**
    * All direct children of this node.
    */
  def children: Seq[XmlTraverser] = {
    val arrayResult = new ArrayBuffer[XmlTraverser]
    val child = node.child
    var idx = 0
    while(idx < child.length) {
      if(!child(idx).isInstanceOf[InMemoryXmlText]) {
        arrayResult.append(XmlTraverser(child(idx), Some(this), uniqueFileId))
      }
      idx += 1
    }
    arrayResult
  }

  /**
    * All direct and indirect children of this node.
    */
  def childrenRecursive: Seq[XmlTraverser] = {
    val results = new ArrayBuffer[XmlTraverser]()
    childrenRecursiveBuild(results)
    results
  }

  private def childrenRecursiveBuild(arrayBuffer: ArrayBuffer[XmlTraverser]): Unit = {
    val c = children
    c foreach(child => arrayBuffer.append(child))
    c foreach(child => child.childrenRecursiveBuild(arrayBuffer))
  }

  /**
    * Lists all parent nodes.
    */
  def parents: List[InMemoryXmlNode] = {
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
      val groupPrefix = if(uniqueFileId != "") s"$uniqueFileId-" else ""
      DataSource.generateEntityUri(groupPrefix + node.label, nodeId)
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
    val attributes = if(depth == 0) Seq() else node.attributes.keys.toSeq
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
    val result = new ArrayBuffer[XmlTraverser]()
    evaluateOperators(path.operators, result)
    result
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
    if(!fetchEntityUri && (node.isInstanceOf[InMemoryXmlText] || (node.child.size == 1 && node.child.head.isInstanceOf[InMemoryXmlText]))) {
      Some(node.text)
    } else if(uriPattern.nonEmpty || fetchEntityUri) {
      Some(generateUri(uriPattern))
    } else {
      None
    }
  }

  private def evaluateOperators(ops: List[PathOperator], results: ArrayBuffer[XmlTraverser]): Unit = {
    ops match {
      case Nil =>
        results.append(this)
      case op :: opsTail =>
        evaluateOperator(op) foreach (_.evaluateOperators(opsTail, results))
    }
  }

  private def evaluateOperator(op: PathOperator): Array[XmlTraverser] = {
    op match {
      case op: ForwardOperator => evaluateForwardOperator(op)
      case op: PropertyFilter => evaluatePropertyFilter(op)
      case op: BackwardOperator => evaluateBackwardOperator(op)
      case _ => throw new UnsupportedOperationException("Unsupported path operator: " + op.getClass.getSimpleName)
    }
  }

  private def evaluateForwardOperator(op: ForwardOperator): Array[XmlTraverser] = {
    op.property.uri match {
      case "#id" =>
        asArray(XmlTraverser(InMemoryXmlText(nodeId), Some(this), uniqueFileId))
      case "#tag" =>
        asArray(XmlTraverser(InMemoryXmlText(node.label), Some(this), uniqueFileId))
      case "#text" =>
        asArray(this)
      case "*" =>
        children.toArray
      case "**" =>
        childrenRecursive.toArray
      case uri: String if uri.startsWith("@") =>
        node.attributes.get(uri.tail) match {
          case Some(attrValue) =>
            asArray(XmlTraverser(InMemoryXmlText(attrValue), Some(this), uniqueFileId))
          case None =>
            XmlTraverser.emptyArray
        }
      case uri: String =>
        val cs = node.childSelect(uri)
        val result = new Array[XmlTraverser](cs.length)
        var idx = 0
        while(idx < cs.length) {
          result(idx) = XmlTraverser(cs(idx), Some(this), uniqueFileId)
          idx += 1
        }
        result
    }
  }

  private def evaluateBackwardOperator(op: BackwardOperator): Array[XmlTraverser] = {
    parentOpt match {
      case Some(parent) =>
        asArray(parent)
      case None =>
        throw new RuntimeException("Cannot go backward from root XML element! Backward property: " + op.property.uri)
    }
  }

  def asArray(xmlTraverser: XmlTraverser): Array[XmlTraverser] = {
    val arr = new Array[XmlTraverser](1)
    arr(0) = xmlTraverser
    arr
  }

  private def evaluatePropertyFilter(op: PropertyFilter): Array[XmlTraverser] = {
    val nodeArray = node.asArray
    var idx = 0
    while(idx < nodeArray.length) {
      if(op.evaluate("\"" + (InMemoryXmlNodes(nodeArray(idx).childSelect(op.property.uri)).text) + "\"")) {
        return asArray(this)
      }
      idx += 1
    }
    new Array[XmlTraverser](0)
  }
}

object XmlTraverser {

  private val uriRegex = "\\{([^\\}]+)\\}".r

  /**
    * Generates a ID for a given XML node that is unique inside the document.
    */
  private def nodeId(node: InMemoryXmlNode): String = {
    node.id
  }

  def apply(node: Node, uniqueFileId: String): XmlTraverser = XmlTraverser(InMemoryXmlNode.fromNode(node), uniqueFileId = uniqueFileId)

  val emptySeq: Seq[XmlTraverser] = Seq.empty
  val emptyArray: Array[XmlTraverser] = Array.empty
}