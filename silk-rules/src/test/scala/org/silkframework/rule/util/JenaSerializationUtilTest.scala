package org.silkframework.rule.util

import org.apache.jena.datatypes.xsd.XSDDatatype
import org.apache.jena.graph.NodeFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class JenaSerializationUtilTest extends AnyFlatSpec with Matchers {
  behavior of "Jena Serialization Util"

  it should "serialize single RDF nodes" in {
    JenaSerializationUtil.serializeSingleNode(NodeFactory.createLiteralString("some string")) mustBe "\"some string\""
    JenaSerializationUtil.serializeSingleNode(NodeFactory.createURI("urn:test:test1")) mustBe "<urn:test:test1>"
    JenaSerializationUtil.serializeSingleNode(NodeFactory.createLiteralLang("value", "en")) mustBe "\"value\"@en"
    JenaSerializationUtil.serializeSingleNode(NodeFactory.createLiteralDT("42.23", XSDDatatype.XSDdecimal)) mustBe "\"42.23\"^^<http://www.w3.org/2001/XMLSchema#decimal>"
  }
}
