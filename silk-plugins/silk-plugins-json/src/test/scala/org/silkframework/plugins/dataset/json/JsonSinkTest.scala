package org.silkframework.plugins.dataset.json

import org.scalatest.{FlatSpec, Matchers}
import org.silkframework.config.Prefixes
import org.silkframework.entity.paths.{TypedPath, UntypedPath}
import org.silkframework.entity.{Entity, EntitySchema, ValueType}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.InMemoryResourceManager
import play.api.libs.json.Json

class JsonSinkTest extends FlatSpec with Matchers {

  behavior of "JsonSink"

  it should "write nested structures" in {
    val personSchema =
      EntitySchema(
        typeUri = "",
        typedPaths =
          IndexedSeq(
            TypedPath(UntypedPath("id"), ValueType.STRING, isAttribute = true),
            TypedPath(UntypedPath("Name"), ValueType.URI, isAttribute = false),
            TypedPath(UntypedPath("Year"), ValueType.INT, isAttribute = true)
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
            TypedPath(UntypedPath("FirstName"), ValueType.STRING, isAttribute = true),
            TypedPath(UntypedPath("LastName"), ValueType.STRING, isAttribute = true)
          )
      )

    val names = Seq(
      Entity("urn:instance:PersonName1a", IndexedSeq(Seq("John"), Seq("Doe")), nameSchema),
      Entity("urn:instance:PersonName1b", IndexedSeq(Seq("Peter"), Seq("Stein")), nameSchema),
      Entity("urn:instance:PersonName2", IndexedSeq(Seq("Max"), Seq("Mustermann")), nameSchema)
    )

    test(
      entityTables = Seq(persons, names),
      expected = """[
                   |  {
                   |    "id":"001",
                   |    "Name":[
                   |      {
                   |        "FirstName":"John",
                   |        "LastName":"Doe"
                   |      },
                   |      {
                   |        "FirstName":"Peter",
                   |        "LastName":"Stein"
                   |      }
                   |    ],
                   |    "Year": 1980
                   |  },
                   |  {
                   |    "id":"002",
                   |    "Name":[
                   |      {
                   |        "FirstName":"Max",
                   |        "LastName":"Mustermann"
                   |      }
                   |    ],
                   |    "Year": 1990
                   |  }
                   |]""".stripMargin
    )
  }

  private def test(entityTables: Seq[Seq[Entity]], expected: String): Unit = {
    implicit val userContext: UserContext = UserContext.Empty
    implicit val prefixes: Prefixes = Prefixes.empty

    val resource = InMemoryResourceManager().get("temp")
    val sink = new JsonSink(resource, topLevelObject = true)

    for (entityTable <- entityTables) {
      val schema = entityTable.head.schema
      sink.openTable(schema.typeUri, schema.typedPaths.flatMap(_.property))
      for (entity <- entityTable) {
        sink.writeEntity(entity.uri, entity.values)
      }
      sink.closeTable()
    }
    sink.close()

    val actualJson = resource.read(Json.parse)
    val expectedJson = Json.parse(expected)

    actualJson shouldBe expectedJson
  }

}
