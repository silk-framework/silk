package org.silkframework.plugins.dataset.xml

import org.silkframework.entity.{ForwardOperator, PathOperator}

import scala.xml.Node

/**
  * Implementation of XML access functions.
  */
object XmlParser {

  /**
    * Collects all direct and indirect paths from an xml node
    * @param node The xml node to search paths in
    * @param prefix Path prefix to be prepended to all found paths
    * @return Sequence of all found paths
    */
  def collectPaths(node: Node, prefix: Seq[PathOperator] = Nil): Seq[Seq[PathOperator]] = {
    // Generate a path from the xml node itself
    val path = prefix :+ ForwardOperator(node.label)
    // Generate paths for all children nodes
    val childNodes = node \ "_"
    val childPaths = childNodes.flatMap(child => collectPaths(child, path))
    // Generate paths for all attributes
    val attributes = node.attributes.asAttrMap.keys.toSeq
    val attributesPaths = attributes.map(attribute => path :+ ForwardOperator("@" + attribute))

    // We only want to generate paths for leave nodes
    if (childPaths.isEmpty) Seq(path) ++ attributesPaths else attributesPaths ++ childPaths
  }

}
