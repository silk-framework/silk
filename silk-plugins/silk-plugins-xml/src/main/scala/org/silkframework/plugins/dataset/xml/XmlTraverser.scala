package org.silkframework.plugins.dataset.xml

import java.net.URLEncoder

import org.silkframework.dataset.DataSource
import org.silkframework.entity._
import org.silkframework.entity.paths._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.xml.Node

/**
  * Implementation of XML access functions.
  */
case class XmlTraverser(node: InMemoryXmlNode, parentOpt: Option[XmlTraverser] = None) {
  /**
    * All direct children of this node.
    */
  def children: Seq[XmlTraverser] = {
    val arrayResult = new ArrayBuffer[XmlTraverser]
    val child = node.child
    var idx = 0
    while(idx < child.length) {
      if(!child(idx).isInstanceOf[InMemoryXmlText]) {
        arrayResult.append(XmlTraverser(child(idx), Some(this)))
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
    if(!fetchEntityUri && (node.isInstanceOf[InMemoryXmlText] || (node.child.length == 1 && node.child.head.isInstanceOf[InMemoryXmlText]))) {
      Some(node.text)
    } else if(uriPattern.nonEmpty || fetchEntityUri) {
      Some(generateUri(uriPattern))
    } else {
      None
    }
  }

  private def evaluateOperators(ops: List[PathOperator]): Seq[XmlTraverser]  = {
    var current = ArrayBuffer(this)
    var next = new ArrayBuffer[XmlTraverser]()
    for(op <- ops) {
      var idx = 0
      while(idx < current.length) {
        current(idx).evaluateOperator(op, next)
        idx += 1
      }

      val temp = current
      current = next
      next = temp
      next.clear()
    }
    current
  }

  private def evaluateOperator(op: PathOperator, buffer: mutable.Buffer[XmlTraverser]): Unit = {
    op match {
      case op: ForwardOperator => evaluateForwardOperator(op, buffer)
      case op: PropertyFilter => evaluatePropertyFilter(op, buffer)
      case op: BackwardOperator => evaluateBackwardOperator(op, buffer)
      case _ => throw new UnsupportedOperationException("Unsupported path operator: " + op.getClass.getSimpleName)
    }
  }

  private def evaluateForwardOperator(op: ForwardOperator, buffer: mutable.Buffer[XmlTraverser]): Unit= {
    op.property.uri match {
      case "#id" =>
        buffer += XmlTraverser(InMemoryXmlText(nodeId), Some(this))
      case "#tag" =>
        buffer += XmlTraverser(InMemoryXmlText(node.label), Some(this))
      case "#text" =>
        buffer += this
      case "*" =>
        buffer ++= children
      case "**" =>
        buffer ++= childrenRecursive
      case uri: String if uri.startsWith("@") =>
        node.attributes.get(uri.tail) match {
          case Some(attrValue) =>
            buffer += XmlTraverser(InMemoryXmlText(attrValue), Some(this))
          case None =>
            // Nothing to add
        }
      case uri: String =>
        buffer ++= childSelect(uri)
    }
  }

  @inline
  private def childSelect(selector: String): Array[XmlTraverser] = {
    def fail = throw new IllegalArgumentException(selector)

    selector match {
      case "" => fail
      case "_" => selectChildren(!_.isInstanceOf[InMemoryXmlText])
      case _ if selector(0) == '@' && !node.isInstanceOf[InMemoryXmlNodes] => node match {
        case elem: InMemoryXmlElem =>
            elem.attributes.get(selector.drop(1)) match {
              case Some(attrValue) =>
                val arr = new Array[XmlTraverser](1)
                arr(0) = XmlTraverser(InMemoryXmlText(attrValue), Some(this))
                arr
              case None =>
                new Array[XmlTraverser](0)
            }
        case _ => new Array[XmlTraverser](0)
      }
      case _ => selectChildrenByLabel(selector)
    }
  }

  @inline
  private def selectChildrenByLabel(label: String): Array[XmlTraverser] = {
    val arr = node.child

    var idx = 0
    var count = 0
    while(idx < arr.length) {
      if(arr(idx).label == label) {
        count += 1
      }
      idx += 1
    }

    val result = new Array[XmlTraverser](count)
    idx = 0
    var targetIndex = 0
    while(idx < arr.length) {
      if(arr(idx).label == label) {
        result(targetIndex) = XmlTraverser(arr(idx), Some(this))
        targetIndex += 1
      }
      idx += 1
    }
    result
  }

  @inline
  private def selectChildren(cond: InMemoryXmlNode => Boolean): Array[XmlTraverser] = {
    val arr = node.child

    var idx = 0
    var count = 0
    while(idx < arr.length) {
      if(cond(arr(idx))) {
        count += 1
      }
      idx += 1
    }

    val result = new Array[XmlTraverser](count)
    idx = 0
    var targetIndex = 0
    while(idx < arr.length) {
      if(cond(arr(idx))) {
        result(targetIndex) = XmlTraverser(arr(idx), Some(this))
        targetIndex += 1
      }
      idx += 1
    }
    result
  }

  private def evaluateBackwardOperator(op: BackwardOperator, buffer: mutable.Buffer[XmlTraverser]): Unit = {
    parentOpt match {
      case Some(parent) =>
        buffer += parent
      case None =>
        throw new RuntimeException("Cannot go backward from root XML element! Backward property: " + op.property.uri)
    }
  }

  def asArray(xmlTraverser: XmlTraverser): Array[XmlTraverser] = {
    val arr = new Array[XmlTraverser](1)
    arr(0) = xmlTraverser
    arr
  }

  private def evaluatePropertyFilter(op: PropertyFilter, buffer: mutable.Buffer[XmlTraverser]): Unit= {
    val nodeArray = node.child
    var idx = 0
    while(idx < nodeArray.length) {
      if(nodeArray(idx).label == op.property.uri && op.evaluate("\"" + nodeArray(idx).text + "\"")) {
        buffer += this
        return
      }
      idx += 1
    }
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

  def apply(node: Node): XmlTraverser = XmlTraverser(InMemoryXmlNode.fromNode(node))

  val emptySeq: Seq[XmlTraverser] = Seq.empty
  val emptyArray: Array[XmlTraverser] = Array.empty
}