package org.silkframework.runtime.serialization

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.xml.sax.InputSource
import org.xml.sax.helpers.DefaultHandler

import java.io.{ByteArrayInputStream, StringReader}
import java.nio.charset.StandardCharsets
import javax.xml.stream.XMLStreamConstants

class SecureXmlParsingTest extends AnyFlatSpec with Matchers {

  behavior of "SecureXmlParsing"

  // A DTD with an internal entity. If DTDs are processed, '&secret;' expands to the marker below, which a parser would
  // otherwise make readable. If DTDs are rejected, the marker is never expanded. This isolates DTD/entity processing from
  // the JDK's separate policy for accessing external resources, so the test cleanly detects whether hardening is in effect.
  private val secretMarker = "ENTITY-WAS-EXPANDED"
  private val dtdPayload =
    s"""<?xml version="1.0"?>
       |<!DOCTYPE root [<!ENTITY secret "$secretMarker">]>
       |<root>&secret;</root>""".stripMargin

  it should "not process DTDs in the streaming XML input factory" in {
    val reader = SecureXmlParsing.xmlInputFactory().createXMLStreamReader(
      new ByteArrayInputStream(dtdPayload.getBytes(StandardCharsets.UTF_8)))
    val text = new StringBuilder
    // Reading must either reject the DTD or leave the entity unexpanded, but never yield the marker
    try {
      while (reader.hasNext) {
        if (reader.next() == XMLStreamConstants.CHARACTERS) {
          text.append(reader.getText)
        }
      }
    } catch {
      case _: Exception => // rejecting the DTD is the expected hardened behaviour
    }
    text.toString must not include secretMarker
  }

  it should "not process DTDs in the SAX parser factory" in {
    val handler = new DefaultHandler {
      val text = new StringBuilder
      override def characters(ch: Array[Char], start: Int, length: Int): Unit = text.appendAll(ch, start, length)
    }
    try {
      SecureXmlParsing.saxParserFactory().newSAXParser().parse(new InputSource(new StringReader(dtdPayload)), handler)
    } catch {
      case _: Exception => // rejecting the DTD is the expected hardened behaviour
    }
    handler.text.toString must not include secretMarker
  }
}
