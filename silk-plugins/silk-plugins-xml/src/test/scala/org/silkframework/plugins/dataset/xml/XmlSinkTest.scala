package org.silkframework.plugins.dataset.xml

import org.scalatest.{FlatSpec, ShouldMatchers}
import org.silkframework.entity.{Entity, _}
import org.silkframework.runtime.resource.InMemoryResourceManager
import scala.xml.{Node, PrettyPrinter}

class XmlSinkTest extends FlatSpec with ShouldMatchers {

  behavior of "XmlSink"

  it should "write flat structures" in {
    val schema =
      EntitySchema(
        typeUri = "",
        typedPaths =
          IndexedSeq(
            TypedPath(Path("FirstTag"), StringValueType),
            TypedPath(Path("SecondTag"), StringValueType)
          )
      )

    val entities = Seq(Entity("someUri", IndexedSeq(Seq("1"), Seq("2")), schema))

    test(
      basePath = "/Root",
      entityTables = Seq(entities),
      expected =
        <Root xmlns="urn:schema:">
          <FirstTag>1</FirstTag>
          <SecondTag>2</SecondTag>
        </Root>
    )
  }

  it should "write nested structures" in {
    val personSchema =
      EntitySchema(
        typeUri = "",
        typedPaths =
          IndexedSeq(
            TypedPath(Path("id"), StringValueType, isAttribute = true),
            TypedPath(Path("Name"), UriValueType),
            TypedPath(Path("Year"), StringValueType)
          )
      )

    val persons = Seq(
      Entity("urn:instance:Person1", IndexedSeq(Seq("001"), Seq("urn:instance:PersonName1a", "urn:instance:PersonName1b"), Seq("1980")), personSchema),
      Entity("urn:instance:Person2", IndexedSeq(Seq("002"), Seq("urn:instance:PersonName2"), Seq("1990")), personSchema)
    )

    val nameSchema =
      EntitySchema(
        typeUri = "",
        typedPaths =
          IndexedSeq(
            TypedPath(Path("FirstName"), StringValueType),
            TypedPath(Path("LastName"), StringValueType)
          )
      )

    val names = Seq(
      Entity("urn:instance:PersonName1a", IndexedSeq(Seq("John"), Seq("Doe")), nameSchema),
      Entity("urn:instance:PersonName1b", IndexedSeq(Seq("Peter"), Seq("Stein")), nameSchema),
      Entity("urn:instance:PersonName2", IndexedSeq(Seq("Max"), Seq("Mustermann")), nameSchema)
    )

    test(
      basePath = "/Persons/Person",
      entityTables = Seq(persons, names),
      expected =
        <Persons xmlns="urn:schema:">
          <Person id="001">
            <Name>
              <FirstName>John</FirstName>
              <LastName>Doe</LastName>
            </Name>
            <Name>
              <FirstName>Peter</FirstName>
              <LastName>Stein</LastName>
            </Name>
            <Year>1980</Year>
          </Person>
          <Person id="002">
            <Name>
              <FirstName>Max</FirstName>
              <LastName>Mustermann</LastName>
            </Name>
            <Year>1990</Year>
          </Person>
        </Persons>
    )
  }

  private def test(basePath: String, entityTables: Seq[Seq[Entity]], expected: Node): Unit = {
    // Create in-memory XML sink
    val resourceMgr = InMemoryResourceManager()
    val resource = resourceMgr.get("test.xml")
    val sink = new XmlSink(resource, basePath)

    // Write entity tables
    for(entityTable <- entityTables) {
      val schema = entityTable.head.desc
      sink.openTable(schema.typeUri, schema.typedPaths.flatMap(_.property))
      for (entity <- entityTable) {
        sink.writeEntity(entity.uri, entity.values)
      }
      sink.closeTable()
    }
    sink.close()

    // Compare results
    val header = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n"
    val prettyPrinter = new PrettyPrinter(Int.MaxValue, 2)
    val formattedXml = header + prettyPrinter.format(expected)

    resource.loadAsString shouldBe formattedXml
  }

}
