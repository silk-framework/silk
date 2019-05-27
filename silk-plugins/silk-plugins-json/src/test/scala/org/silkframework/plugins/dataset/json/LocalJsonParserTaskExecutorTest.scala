package org.silkframework.plugins.dataset.json

import org.scalatest.mock.MockitoSugar
import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.config.PlainTask
import org.silkframework.entity.{Entity, EntitySchema, Path}
import org.silkframework.execution.local.{GenericEntityTable, LocalExecution}
import org.silkframework.runtime.activity.TestUserContextTrait

class LocalJsonParserTaskExecutorTest extends FlatSpec with MustMatchers with MockitoSugar with TestUserContextTrait {
  behavior of "Local JSON Parser Task Executor"

  it should "parse the JSON and allow entity schema requests against it" in {
    val executor = LocalJsonParserTaskExecutor()
    val jsonParserTask = JsonParserTask("jsonContent", "persons")
    val task = PlainTask("JsonParser", jsonParserTask)
    val entitySchema = EntitySchema("type", IndexedSeq(Path("id"), Path("jsonContent")).map(_.asStringTypedPath))
    val jsonContent =
      """{
        |  "rootId": 1,
        |  "persons": [
        |    {
        |      "id": "0",
        |      "name": "John"
        |    },
        |    {
        |      "id": "1",
        |      "name": "Max"
        |    }
        |  ]
        |}
      """.stripMargin
    val entities = Seq(
      Entity("entity1", values = IndexedSeq(Seq("1"), Seq(jsonContent)), schema = entitySchema)
    )
    val inputEntities = GenericEntityTable(entities, entitySchema, task)
    val result = executor.execute(task, Seq(inputEntities),
      outputSchemaOpt = Some(EntitySchema("", IndexedSeq(Path("name")).map(_.asStringTypedPath))),
      execution = LocalExecution(false)
    )
    result mustBe defined
    result.get.entities.map(_.values.flatten.head) mustBe Seq("John", "Max")
  }
}
