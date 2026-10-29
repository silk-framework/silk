package org.silkframework.runtime.serialization

import javax.xml.XMLConstants
import javax.xml.parsers.SAXParserFactory
import javax.xml.stream.XMLInputFactory

/**
  * Creates XML parsers that are hardened against XXE attacks (external entities, DTDs, entity expansion).
  * All XML parsing of potentially untrusted input, e.g. imported project files or request bodies, should go through here.
  */
object SecureXmlParsing {

  /** Disables DTD loading feature of the standard SAX parser. Blocks external entities and entity expansion. */
  private final val DISALLOW_DOCTYPE = "http://apache.org/xml/features/disallow-doctype-decl"

  /** A streaming XML input factory that neither processes DTDs nor resolves external entities. */
  def xmlInputFactory(): XMLInputFactory = {
    val factory = XMLInputFactory.newInstance()
    factory.setProperty(XMLInputFactory.SUPPORT_DTD, false)
    factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false)
    factory
  }

  /** A SAX parser factory that rejects documents containing a DTD, so that no external entities can be resolved. */
  def saxParserFactory(): SAXParserFactory = {
    val factory = SAXParserFactory.newInstance()
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
    factory.setFeature(DISALLOW_DOCTYPE, true)
    factory.setXIncludeAware(false)
    factory
  }
}
