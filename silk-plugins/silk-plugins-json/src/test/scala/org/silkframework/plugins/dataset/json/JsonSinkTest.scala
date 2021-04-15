package org.silkframework.plugins.dataset.json

import org.scalatest.{FlatSpec, Matchers}
import org.silkframework.config.Prefixes
import org.silkframework.entity.paths.{TypedPath, UntypedPath}
import org.silkframework.entity.{Entity, EntitySchema, ValueType}
import org.silkframework.plugins.dataset.hierarchical.MaxDepthExceededException
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.InMemoryResourceManager
import play.api.libs.json.Json

class JsonSinkTest extends FlatSpec with Matchers {

  behavior of "JsonSink"

  it should "write entities to json using the first object as the root" in {
    val schema =
      EntitySchema(
        typeUri = "",
        typedPaths =
          IndexedSeq(
            TypedPath(UntypedPath("key"), ValueType.STRING, isAttribute = true),
          )
      )

    val entities = Seq(Entity("someUri", IndexedSeq(Seq("value")), schema))

    test(
      entityTables = Seq(entities),
      outputSingleJsonObject = true,
      template = JsonTemplate("", ""),
      expected = """{
                   |  "key": "value"
                   |}""".stripMargin
    )
  }

  it should "write flat structures nested under root element" in {
    val schema =
      EntitySchema(
        typeUri = "",
        typedPaths =
          IndexedSeq(
            TypedPath(UntypedPath("IntegerValue"), ValueType.INT, isAttribute = true),
            TypedPath(UntypedPath("FloatValue"), ValueType.FLOAT, isAttribute = true),
            TypedPath(UntypedPath("StringValue"), ValueType.STRING, isAttribute = true)
          )
      )

    val entities = Seq(Entity("someUri", IndexedSeq(Seq("1"), Seq("1.0"), Seq("one")), schema))

    test(
      entityTables = Seq(entities),
      expected = """[{
                   |  "IntegerValue": 1,
                   |  "FloatValue": 1.0,
                   |  "StringValue": "one"
                   |}]""".stripMargin
    )
  }

  it should "write entities with arrays to json" in {
    val schema =
      EntitySchema(
        typeUri = "",
        typedPaths =
          IndexedSeq(
            TypedPath(UntypedPath("SingleValue"), ValueType.STRING, isAttribute = true),
            TypedPath(UntypedPath("ArrayValue1"), ValueType.STRING, isAttribute = false),
            TypedPath(UntypedPath("ArrayValue2"), ValueType.STRING, isAttribute = false)
          )
      )

    val entities = Seq(Entity("someUri", IndexedSeq(Seq("a"), Seq("a"), Seq("a", "b")), schema))

    test(
      entityTables = Seq(entities),
      expected = """[{
                   |  "SingleValue": "a",
                   |  "ArrayValue1": ["a"],
                   |  "ArrayValue2": ["a", "b"]
                   |}]""".stripMargin
    )
  }

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

  it should "fail if the written entities contain a recursion" in {
    val schema =
      EntitySchema(
        typeUri = "",
        typedPaths =
          IndexedSeq(
            TypedPath(UntypedPath("path1"), ValueType.URI, isAttribute = false),
            TypedPath(UntypedPath("path2"), ValueType.URI, isAttribute = false)
          )
      )

    val entityTables = Seq(
      Seq(Entity("e1_1", IndexedSeq(Seq("e2_1"), Seq()), schema)),
      Seq(Entity("e2_1", IndexedSeq(Seq("e3_1"), Seq("e3_2")), schema)),
      Seq(Entity("e3_1", IndexedSeq(Seq("e1_1"), Seq()), schema), Entity("e3_2", IndexedSeq(Seq(), Seq("e1_1")), schema)),
    )

    intercept[MaxDepthExceededException] {
      test(
        entityTables = entityTables,
        expected = """should never generate a JSON""".stripMargin
      )
    }
  }

  private def test(entityTables: Seq[Seq[Entity]], outputSingleJsonObject: Boolean = false, template: JsonTemplate = JsonTemplate.default, expected: String): Unit = {
    implicit val userContext: UserContext = UserContext.Empty
    implicit val prefixes: Prefixes = Prefixes.empty

    val resource = InMemoryResourceManager().get("temp")
    val sink = new JsonSink(resource, outputSingleJsonObject, template)

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
