package org.silkframework.plugins.dataset.xml

import org.scalatest.{FlatSpec, ShouldMatchers}
import org.silkframework.dataset.TypedProperty
import org.silkframework.entity.{StringValueType, UriValueType}
import org.silkframework.runtime.resource.{InMemoryResourceManager, Resource}
import org.silkframework.util.Uri

import scala.xml.{Node, PrettyPrinter, XML}

class XmlSinkTest extends FlatSpec with ShouldMatchers {

  behavior of "XmlSink"

  it should "write flat structures" in {
    val resourceMgr = InMemoryResourceManager()
    val resource = resourceMgr.get("test.xml")
    val sink = new XmlSink(resource)

    val properties = Seq(
      TypedProperty("FirstTag", StringValueType, isBackwardProperty = false),
      TypedProperty("SecondTag", StringValueType, isBackwardProperty = false)
    )

    sink.open(Uri("Root"), properties)
    sink.writeEntity("someUri", Seq(Seq("1"), Seq("2")))
    sink.close()

    compareResult(resource,
      <Root>
        <FirstTag>1</FirstTag>
        <SecondTag>2</SecondTag>
      </Root>
    )
  }

  it should "write nested structures" in {
    val resourceMgr = InMemoryResourceManager()
    val resource = resourceMgr.get("test.xml")
    val sink = new XmlSink(resource)

    val properties = Seq(
      TypedProperty("Person", UriValueType, isBackwardProperty = false)
    )
    sink.open(Uri("Persons"), properties)
    sink.writeEntity("someUri", Seq(Seq("urn:instance:Person1")))
    sink.writeEntity("someUri", Seq(Seq("urn:instance:Person2")))
    sink.close()

    val properties2 = Seq(
      TypedProperty("FirstName", StringValueType, isBackwardProperty = false),
      TypedProperty("LastName", StringValueType, isBackwardProperty = false)
    )
    sink.open(Uri(""), properties2)
    sink.writeEntity("urn:instance:Person1", Seq(Seq("John"), Seq("Doe")))
    sink.writeEntity("urn:instance:Person2", Seq(Seq("Max"), Seq("Mustermann")))
    sink.close()

    compareResult(resource,
      <Persons>
        <Person>
          <FirstName>John</FirstName>
          <LastName>Doe</LastName>
        </Person>
        <Person>
          <FirstName>Max</FirstName>
          <LastName>Mustermann</LastName>
        </Person>
      </Persons>
    )
  }

  private def compareResult(resource: Resource, xml: Node): Unit = {
    val prettyPrinter = new PrettyPrinter(Int.MaxValue, 0)
    val formattedXml = prettyPrinter.format(xml)

    "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" + formattedXml shouldBe resource.loadAsString
  }

}
