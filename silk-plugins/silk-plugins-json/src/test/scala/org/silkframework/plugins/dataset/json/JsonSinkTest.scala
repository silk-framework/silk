package org.silkframework.plugins.dataset.json

import java.io.File

import org.scalatest.{FlatSpec, Matchers}
import org.silkframework.config.Prefixes
import org.silkframework.dataset.{DataSource, TypedProperty}
import org.silkframework.entity.{BooleanValueType, Entity, EntitySchema, ValueType}
import org.silkframework.entity.paths.{TypedPath, UntypedPath}
import org.silkframework.runtime.activity.UserContext
import org.silkframework.runtime.resource.{FileResource, InMemoryResourceManager}
import org.silkframework.util.Uri


class JsonSinkTest extends FlatSpec with Matchers {

  implicit val userContext: UserContext = UserContext.Empty
  implicit val prefixes: Prefixes = Prefixes.empty

  it should "write entities to json" in {
    val tempFile = File.createTempFile("json-write-test-1", ".json")
    tempFile.deleteOnExit()
    val inputEntites = getEntities
    val sink = new JsonSink(FileResource(tempFile), topLevelObject = false)

    val typedProps: Seq[TypedProperty] = inputEntites.head.schema.typedPaths.map(_.property.get)
    sink.openTable("typeUri", typedProps)
    var uri: Int = 0
    for (entity <- inputEntites) {
      sink.writeEntity(Uri(uri.toString), entity.values)
      uri += 1
    }
    sink.closeTable()
    sink.close()

    val source = scala.io.Source.fromFile(tempFile)
    val lines = try source.mkString finally source.close()
    inputEntites.size shouldBe 3
    tempFile.exists() shouldBe(true)
    lines.shouldBe(
      """[{"value": "val"}, {
        |  "boolean": true,
        |  "value": "val2"
        |}, {
        |  "boolean": true,
        |  "value": "val3"
        |}]""".stripMargin
    )
  }

  it should "write entities to json using the first object as the root" in {
    val tempFile = File.createTempFile("json-write-test-2", ".json")
    tempFile.deleteOnExit()
    val inputEntites = getEntities
    val sink = new JsonSink(FileResource(tempFile), topLevelObject = true)
    sink.clear()
    val typedProps: Seq[TypedProperty] = inputEntites.head.schema.typedPaths.map(_.property.get)
    sink.openTable("typeUri", typedProps)
    var uri: Int = 0
    for (entity <- inputEntites) {
      sink.writeEntity(Uri(uri.toString), entity.values)
      uri += 1
    }
    sink.closeTable()
    sink.close()

    val source = scala.io.Source.fromFile(tempFile)
    val lines = try source.mkString finally source.close()

    inputEntites.size shouldBe 3
    tempFile.exists() shouldBe(true)
    lines.shouldBe(
      """{"value": "val"}""".stripMargin
    )
  }

  it should "write entities with arrays to json" in {
    val tempFile = File.createTempFile("json-write-test-3", ".json")
    tempFile.deleteOnExit()
    val inputEntites = getArrayEntities
    val sink = new JsonSink(FileResource(tempFile), topLevelObject = false)

    val typedProps: Seq[TypedProperty] = inputEntites.head.schema.typedPaths.map(_.property.get)
    sink.openTable("typeUri", typedProps)
    var uri: Int = 0
    for (entity <- inputEntites) {
      sink.writeEntity(Uri(uri.toString), entity.values)
      uri += 1
    }
    sink.closeTable()
    sink.close()

    val source = scala.io.Source.fromFile(tempFile)
    val lines = try source.mkString finally source.close()

    inputEntites.size shouldBe 3
    tempFile.exists() shouldBe(true)
    lines.shouldBe(
     """[{}, {"array": [
       |  1,
       |  2,
       |  3
       |]}, {"array": [
       |  1,
       |  2,
       |  3
       |]}]""".stripMargin
    )
  }

  it should "write flat structures nested under root element" in {
    val tempFile = File.createTempFile("json-write-test-4", ".json")
    tempFile.deleteOnExit()
    val schema =
      EntitySchema(
        typeUri = "",
        typedPaths =
          IndexedSeq(
            TypedPath(UntypedPath("FirstTag"), ValueType.STRING, isAttribute = false),
            TypedPath(UntypedPath("SecondTag"), ValueType.STRING, isAttribute = false)
          )
      )

    val entities = Seq(Entity("someUri", IndexedSeq(Seq("1"), Seq("2")), schema))

    test(
      entityTables = Seq(entities),
      expected = """[{
                   |  "SecondTag": 2,
                   |  "FirstTag": 1
                   |}]""".stripMargin,
      tempFile
    )
  }

  it should "write nested structures" in {
    val tempFile = File.createTempFile("json-write-test-5", ".json")
    tempFile.deleteOnExit()
    val personSchema =
      EntitySchema(
        typeUri = "",
        typedPaths =
          IndexedSeq(
            TypedPath(UntypedPath("id"), ValueType.STRING, isAttribute = true),
            TypedPath(UntypedPath("Name"), ValueType.URI, isAttribute = false),
            TypedPath(UntypedPath("Year"), ValueType.STRING, isAttribute = false)
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
            TypedPath(UntypedPath("FirstName"), ValueType.STRING, isAttribute = false),
            TypedPath(UntypedPath("LastName"), ValueType.STRING, isAttribute = false)
          )
      )

    val names = Seq(
      Entity("urn:instance:PersonName1a", IndexedSeq(Seq("John"), Seq("Doe")), nameSchema),
      Entity("urn:instance:PersonName1b", IndexedSeq(Seq("Peter"), Seq("Stein")), nameSchema),
      Entity("urn:instance:PersonName2", IndexedSeq(Seq("Max"), Seq("Mustermann")), nameSchema)
    )

    test(
      entityTables = Seq(persons, names),
      expected = """[{
                   |  "Year": 1980,
                   |  "Name": [
                   |    {
                   |      "FirstName": "John",
                   |      "LastName": "Doe"
                   |    },
                   |    {
                   |      "FirstName": "Peter",
                   |      "LastName": "Stein"
                   |    }
                   |  ]
                   |}, {
                   |  "Year": 1990,
                   |  "Name": {
                   |    "FirstName": "Max",
                   |    "LastName": "Mustermann"
                   |  }
                   |}]""".stripMargin, tempFile
    )
  }

  private def test(entityTables: Seq[Seq[Entity]], expected: String, tempFile: File): Unit = {
    implicit val userContext: UserContext = UserContext.Empty
    implicit val prefixes: Prefixes = Prefixes.empty

    val resource = new FileResource(tempFile)
    val sink = new JsonSink(resource, topLevelObject = false)

    for (entityTable <- entityTables) {
      val schema = entityTable.head.schema
      sink.openTable(schema.typeUri, schema.typedPaths.flatMap(_.property))
      for (entity <- entityTable) {
        sink.writeEntity(entity.uri, entity.values)
      }
      sink.closeTable()
    }
    sink.close()
    val source = scala.io.Source.fromFile(tempFile)
    val lines = try source.mkString finally source.close()
    lines shouldEqual expected
  }

  private val jsonComplex =
    """{ "object": {"blah": 3},
      |  "objects": [
      |    {"value":"val", "nestedObject": {"nestedValue": "nested"}},
      |    null,
      |    {"value": "val2", "boolean": true, "int": 3, "float": 3.41, "emptyObject": {}, "emptyArray": [], "array": [1,2,3], "objectArray": [{"v": 2}]},
      |    {"value": "val3", "boolean": true, "int": 3, "float": 3.41, "emptyObject": {}, "emptyArray": [], "array": [1,2,3], "objectArray": [{"v": 2}]}
      |  ],
      |  "values": ["arr1", "arr2"]
      |}""".stripMargin


  def getEntities: Traversable[Entity] = {
    val source: DataSource = jsonSource(jsonComplex)
    source.retrieve(EntitySchema("objects", typedPaths = IndexedSeq(
      UntypedPath.parse("value").asStringTypedPath,
      TypedPath("boolean", BooleanValueType())
    ))).entities
  }

  def getArrayEntities: Traversable[Entity] = {
    val source: DataSource = jsonSource(jsonComplex)
    source.retrieve(EntitySchema("objects", typedPaths = IndexedSeq(
      UntypedPath.parse("array").asStringTypedPath
    ))).entities

  }

  private def jsonSource(json: String): JsonSource = {
    val jsonResource = InMemoryResourceManager().get("temp.json")
    jsonResource.writeString(json)
    val source = JsonDataset(jsonResource).source
    source.asInstanceOf[JsonSource]
  }
}
