package org.silkframework.rule.util

import org.apache.jena.graph.{Node, NodeFactory, Triple => JenaTriple}
import org.apache.jena.riot.RDFDataMgr

import java.io.ByteArrayOutputStream
import scala.collection.JavaConverters._

/**
  * Some helper methods to serialize Jena objects to strings.
  */
object JenaSerializationUtil {
  /**
    * Serialize a single triple.
    *
    * @param subject Subject URI
    * @param property Property URI
    * @param node A Jena [[Node]].
    * @return
    */
  def serializeTriple(subject: String, property: String, node: Node): String = {
    val output = new ByteArrayOutputStream()
    val triple = new JenaTriple(NodeFactory.createURI(subject), NodeFactory.createURI(property), node)
    RDFDataMgr.writeTriples(output, Iterator(triple).asJava)
    output.toString("UTF-8")
  }

  def serializeSingleNode(node: Node): String = {
    val subjectPropertyLength = 8
    val spaceDotNewLineLength = 3
    val output = new ByteArrayOutputStream()
    val triple = new JenaTriple(NodeFactory.createURI("a"), NodeFactory.createURI("b"), node)
    RDFDataMgr.writeTriples(output, Iterator(triple).asJava)
    output.toString("UTF-8").drop(subjectPropertyLength).dropRight(spaceDotNewLineLength)
  }
}
