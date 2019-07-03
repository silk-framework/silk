package org.silkframework.plugins.dataset.xml

import org.scalatest.{FlatSpec, Matchers}
import org.silkframework.entity.{Entity, _}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.InMemoryResourceManager

import scala.xml.{Node, PrettyPrinter, XML}

class XmlSinkTest extends FlatSpec with Matchers {

  behavior of "XmlSink"

  it should "write flat structures nested under root element" in {
    val schema =
      EntitySchema(
        typeUri = "",
        typedPaths =
          IndexedSeq(
            TypedPath(UntypedPath("FirstTag"), StringValueType, isAttribute = false),
            TypedPath(UntypedPath("SecondTag"), StringValueType, isAttribute = false)
          )
      )

    val entities = Seq(Entity("someUri", IndexedSeq(Seq("1"), Seq("2")), schema))

    test(
      template = "<Root><?Element?></Root>",
      entityTables = Seq(entities),
      expected =
        <Root>
          <Element>
            <FirstTag>1</FirstTag>
            <SecondTag>2</SecondTag>
          </Element>
        </Root>
    )
  }

  it should "write flat structures in complex XML template" in {
    val schema =
      EntitySchema(
        typeUri = "",
        typedPaths =
            IndexedSeq(
              TypedPath(UntypedPath("FirstTag"), StringValueType, isAttribute = false),
              TypedPath(UntypedPath("SecondTag"), StringValueType, isAttribute = false)
            )
      )

    val entities = Seq(Entity("someUri", IndexedSeq(Seq("1"), Seq("2")), schema))

    test(
      template = """<Root><OtherElement id="other">Other Element</OtherElement><NestedElement><ID>1</ID><?Element?></NestedElement></Root>""",
      entityTables = Seq(entities),
      expected =
          <Root>
            <OtherElement id="other">Other Element</OtherElement>
            <NestedElement>
              <ID>1</ID>
              <Element>
                <FirstTag>1</FirstTag>
                <SecondTag>2</SecondTag>
              </Element>
            </NestedElement>
          </Root>
    )
  }

  it should "write entities as root element" in {
    val schema =
      EntitySchema(
        typeUri = "",
        typedPaths =
          IndexedSeq(
            TypedPath(UntypedPath("FirstTag"), StringValueType, isAttribute = false),
            TypedPath(UntypedPath("SecondTag"), StringValueType, isAttribute = false)
          )
      )

    val entities = Seq(Entity("someUri", IndexedSeq(Seq("1"), Seq("2")), schema))

    test(
      template = "<?Element?>",
      entityTables = Seq(entities),
      expected =
        <Element>
          <FirstTag>1</FirstTag>
          <SecondTag>2</SecondTag>
        </Element>
    )
  }

  it should "write attributes" in {
    val schema =
      EntitySchema(
        typeUri = "",
        typedPaths =
          IndexedSeq(
            TypedPath(UntypedPath("http://example1.org/id"), StringValueType, isAttribute = true),
            TypedPath(UntypedPath("http://example2.org/id"), StringValueType, isAttribute = true)
          )
      )

    val entities = Seq(Entity("someUri", IndexedSeq(Seq("101"), Seq("102")), schema))

    test(
      template = "<Root><?Element?></Root>",
      entityTables = Seq(entities),
      expected =
        <Root xmlns="urn:schema:">
          <Element xmlns:ns1="http://example1.org/" ns1:id="101" xmlns:ns0="http://example2.org/" ns0:id="102" />
        </Root>
    )
  }

  it should "write elements with attributes" in {
    val schema =
      EntitySchema(
        typeUri = "",
        typedPaths =
          IndexedSeq(
            TypedPath(UntypedPath("http://example1.org/id"), StringValueType, isAttribute = true),
            TypedPath(UntypedPath("http://example2.org/id"), StringValueType, isAttribute = true),
            TypedPath(UntypedPath("#text"), StringValueType, isAttribute = false)
          )
      )

    val entities = Seq(Entity("someUri", IndexedSeq(Seq("101"), Seq("102"), Seq("Value")), schema))

    test(
      template = "<Root><?Element?></Root>",
      entityTables = Seq(entities),
      expected =
        <Root xmlns="urn:schema:">
          <Element xmlns:ns1="http://example1.org/" ns1:id="101" xmlns:ns0="http://example2.org/" ns0:id="102">Value</Element>
        </Root>
    )
  }

  it should "use existing namespaces from the template" in {
    val schema =
      EntitySchema(
        typeUri = "",
        typedPaths =
          IndexedSeq(
            TypedPath(UntypedPath("http://example1.org/FirstTag"), StringValueType, isAttribute = false),
            TypedPath(UntypedPath("http://example1.org/SecondTag"), StringValueType, isAttribute = false)
          )
      )

    val entities = Seq(Entity("someUri", IndexedSeq(Seq("1"), Seq("2")), schema))

    test(
      template =
        """<Root xmlns="http://example1.org/"><?Element?></Root>""",
      entityTables = Seq(entities),
      expected =
        <Root xmlns="http://example1.org/">
          <Element>
            <FirstTag>1</FirstTag>
            <SecondTag>2</SecondTag>
          </Element>
        </Root>
    )
  }

  it should "write nested structures" in {
    val personSchema =
      EntitySchema(
        typeUri = "",
        typedPaths =
          IndexedSeq(
            TypedPath(UntypedPath("id"), StringValueType, isAttribute = true),
            TypedPath(UntypedPath("Name"), UriValueType, isAttribute = false),
            TypedPath(UntypedPath("Year"), StringValueType, isAttribute = false)
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
            TypedPath(UntypedPath("FirstName"), StringValueType, isAttribute = false),
            TypedPath(UntypedPath("LastName"), StringValueType, isAttribute = false)
          )
      )

    val names = Seq(
      Entity("urn:instance:PersonName1a", IndexedSeq(Seq("John"), Seq("Doe")), nameSchema),
      Entity("urn:instance:PersonName1b", IndexedSeq(Seq("Peter"), Seq("Stein")), nameSchema),
      Entity("urn:instance:PersonName2", IndexedSeq(Seq("Max"), Seq("Mustermann")), nameSchema)
    )

    test(
      template = "<Persons><?Person?></Persons>",
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

  it should "write nested structure as root element" in {
    val personSchema =
      EntitySchema(
        typeUri = "",
        typedPaths =
          IndexedSeq(
            TypedPath(UntypedPath("id"), StringValueType, isAttribute = true),
            TypedPath(UntypedPath("Name"), UriValueType, isAttribute = false),
            TypedPath(UntypedPath("Year"), StringValueType, isAttribute = false)
          )
      )

    val persons = Seq(
      Entity("urn:instance:Person1", IndexedSeq(Seq("001"), Seq("urn:instance:PersonName1a", "urn:instance:PersonName1b"), Seq("1980")), personSchema)
    )

    val nameSchema =
      EntitySchema(
        typeUri = "",
        typedPaths =
          IndexedSeq(
            TypedPath(UntypedPath("FirstName"), StringValueType, isAttribute = false),
            TypedPath(UntypedPath("LastName"), StringValueType, isAttribute = false)
          )
      )

    val names = Seq(
      Entity("urn:instance:PersonName1a", IndexedSeq(Seq("John"), Seq("Doe")), nameSchema),
      Entity("urn:instance:PersonName1b", IndexedSeq(Seq("Peter"), Seq("Stein")), nameSchema)
    )

    test(
      template = "<?Person?>",
      entityTables = Seq(persons, names),
      expected =
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
    )
  }

  private def test(template: String, entityTables: Seq[Seq[Entity]], expected: Node): Unit = {
    implicit val userContext: UserContext = UserContext.Empty
    // Create in-memory XML sink
    val resourceMgr = InMemoryResourceManager()
    val resource = resourceMgr.get("test.xml")
    val sink = new XmlSink(resource, template)

    // Write entity tables
    for(entityTable <- entityTables) {
      val schema = entityTable.head.schema
      sink.openTable(schema.typeUri, schema.typedPaths.flatMap(_.property))
      for (entity <- entityTable) {
        sink.writeEntity(entity.uri, entity.values)
      }
      sink.closeTable()
    }
    sink.close()

    // We need to reformat the expected XML to normalize the formatting.
    val prettyPrinter = new PrettyPrinter(Int.MaxValue, 2)
    val formattedXml = prettyPrinter.format(expected)
    val formattedExpected = XML.loadString(formattedXml)

    resource.read(XML.load) shouldBe formattedExpected
  }

}
