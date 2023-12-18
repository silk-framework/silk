package org.silkframework.plugins.dataset.xml

import org.silkframework.dataset.DataSource
import org.silkframework.entity._
import org.silkframework.entity.paths._
import org.silkframework.util.Identifier

import java.net.URLEncoder
import scala.collection.mutable.ArrayBuffer

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
    arrayResult.toSeq
  }

  /**
    * All direct and indirect children of this node.
    */
  def childrenRecursive: Seq[XmlTraverser] = {
    val results = new ArrayBuffer[XmlTraverser]()
    childrenRecursiveBuild(results)
    results.toSeq
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
      DataSource.generateEntityUri(Identifier.fromAllowed(node.label), nodeId)
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
    val attributesPaths = attributes.map(attribute => TypedPath((path :+ ForwardOperator("@" + attribute)).toList, ValueType.STRING, isAttribute = true))
    // Paths to inner nodes become object paths (URI), else value paths (string)
    val pathValueType: ValueType = if(children.nonEmpty || node.attributes.nonEmpty) ValueType.URI else ValueType.STRING
    val typedPath = TypedPath(path.toList, pathValueType, isAttribute = false)

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
  def evaluatePath(path: UntypedPath): IndexedSeq[XmlTraverser] = {
    evaluateOperators(path.operators)
  }

  /**
    * Evaluates a Silk path on this node and returns the node values as strings.
    *
    * @param path A path relative to the given XML node.
    * @return A sequence of nodes that are matching the path.
    */
  def evaluatePathAsString(path: TypedPath, uriPattern: String): IndexedSeq[String] = {
    val fetchEntityUri = path.valueType == ValueType.URI
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
    if(!fetchEntityUri &&
      (node.isInstanceOf[InMemoryXmlText] || node.isInstanceOf[InMemoryXmlAttribute] || (node.child.length >= 1 && node.child.forall(_.isInstanceOf[InMemoryXmlText])))) {
      Some(node.text)
    } else if(uriPattern.nonEmpty || fetchEntityUri) {
      Some(generateUri(uriPattern))
    } else {
      None
    }
  }

  private def evaluateOperators(ops: List[PathOperator]): IndexedSeq[XmlTraverser]  = {
    var current = new ArrayBuffer[XmlTraverser]()
    current += this
    var next = new ArrayBuffer[XmlTraverser]()

    for(op <- ops) {
      var idx = 0
      while(idx < current.size) {
        current(idx).evaluateOperator(op, next)
        idx += 1
      }

      // Switch buffers
      val temp = current
      current = next
      next = temp
      next.clear()
    }
    current.toIndexedSeq
  }

  private def evaluateOperator(op: PathOperator, buffer: ArrayBuffer[XmlTraverser]): Unit = {
    op match {
      case op: ForwardOperator => evaluateForwardOperator(op, buffer)
      case op: PropertyFilter => evaluatePropertyFilter(op, buffer)
      case op: BackwardOperator => evaluateBackwardOperator(op, buffer)
      case _ => throw new UnsupportedOperationException("Unsupported path operator: " + op.getClass.getSimpleName)
    }
  }

  private def evaluateForwardOperator(op: ForwardOperator, buffer: ArrayBuffer[XmlTraverser]): Unit= {
    op.property.uri match {
      case "#id" =>
        buffer += XmlTraverser(InMemoryXmlText(nodeId, node.position), Some(this))
      case "#tag" =>
        buffer += XmlTraverser(InMemoryXmlText(node.label, node.position), Some(this))
      case "#text" =>
        buffer += XmlTraverser(InMemoryXmlText(node.text, node.position), Some(this))
      case "#line" =>
        buffer += XmlTraverser(InMemoryXmlText(node.position.line.toString, node.position), Some(this))
      case "#column" =>
        buffer += XmlTraverser(InMemoryXmlText(node.position.column.toString, node.position), Some(this))
      case "*" =>
        buffer ++= children
      case "**" =>
        buffer ++= childrenRecursive
      case uri: String if uri.startsWith("@") =>
        node.attributes.get(uri.tail) match {
          case Some(attrValue) =>
            buffer += XmlTraverser(InMemoryXmlAttribute(
              attributeName = uri,
              value = attrValue,
              node.position
            ), Some(this))
          case None =>
            // Nothing to add
        }
      case uri: String =>
        childSelect(uri, buffer)
    }
  }

  /** Projection function, which returns  elements of `this` sequence based
    *  on the string `that`. Use:
    *   - `this \ "foo"` to get a list of all elements that are labelled with `"foo"`;
    *   - `\ "_"` to get a list of all elements (wildcard);
    *   - `ns \ "@foo"` to get the unprefixed attribute `"foo"`;
    *   - `ns \ "@{uri}foo"` to get the prefixed attribute `"pre:foo"` whose
    *     prefix `"pre"` is resolved to the namespace `"uri"`.
    *
    *  For attribute projections, the resulting [[scala.xml.NodeSeq]] attribute
    *  values are wrapped in a [[scala.xml.Group]].
    *
    *  There is no support for searching a prefixed attribute by its literal prefix.
    *
    *  The document order is preserved.
    */
  @inline
  private def childSelect(selector: String, buffer: ArrayBuffer[XmlTraverser]): Unit = {
    def fail = throw new IllegalArgumentException(selector)

    selector match {
      case "" => fail
      case "_" => selectNonTextChildren(buffer)
      case _ if selector(0) == '@' => node match {
        case elem: InMemoryXmlElem =>
          for(attributeValue <- elem.attributes.get(selector.substring(1))) {
            buffer += XmlTraverser(InMemoryXmlAttribute(
              attributeName = selector,
              value = attributeValue,
              position = elem.position
            ), Some(this))
          }
        case _ =>
          // Nothing to add
      }
      case _ => selectChildrenByLabel(selector, buffer)
    }
  }

  @inline
  private def selectNonTextChildren(buffer: ArrayBuffer[XmlTraverser]): Unit = {
    val arr = node.child
    var idx = 0
    while(idx < arr.length) {
      if(!arr(idx).isInstanceOf[InMemoryXmlText]) {
        buffer += XmlTraverser(arr(idx), Some(this))
      }
      idx += 1
    }
  }

  @inline
  private def selectChildrenByLabel(label: String, buffer: ArrayBuffer[XmlTraverser]): Unit = {
    val arr = node.child
    var idx = 0
    while(idx < arr.length) {
      if(arr(idx).label == label) {
        buffer += XmlTraverser(arr(idx), Some(this))
      }
      idx += 1
    }
  }

  private def evaluateBackwardOperator(op: BackwardOperator, buffer: ArrayBuffer[XmlTraverser]): Unit = {
    parentOpt match {
      case Some(parent) =>
        buffer += parent
      case None =>
        throw new RuntimeException("Cannot go backward from root XML element! Backward property: " + op.property.uri)
    }
  }

  private def evaluatePropertyFilter(op: PropertyFilter, buffer: ArrayBuffer[XmlTraverser]): Unit= {
    val nodeArray = node.child
    var idx = 0
    if(op.property.uri.startsWith("@")) {
      node.attributes.get(op.property.uri.drop(1)) foreach { value =>
        if(op.evaluate(s"""\"$value\"""")) {
          buffer += this
        }
      }
    } else {
      while(idx < nodeArray.length) {
        if(nodeArray(idx).label == op.property.uri && op.evaluate(nodeArray(idx).textExpression)) {
          buffer += this
          return
        }
        idx += 1
      }
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



  val emptySeq: Seq[XmlTraverser] = Seq.empty
  val emptyArray: Array[XmlTraverser] = Array.empty
}