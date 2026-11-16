package org.silkframework.rule.execution

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.config.PlainTask
import org.silkframework.dataset.DatasetSpec
import org.silkframework.entity.ValueType
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.execution.ExecutorRegistry
import org.silkframework.plugins.dataset.json.JsonDataset
import org.silkframework.rule.execution.local.MultipleValuesException
import org.silkframework.rule.{DatasetSelection, DirectMapping, MappingRules, MappingTarget, RootMappingRule, TransformSpec}
import org.silkframework.runtime.activity.{Activity, UserContext}
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.runtime.resource.InMemoryResourceManager
import org.silkframework.util.Uri

/**
  * A transformation that fails while it generates its entities, after the output table has been opened,
  * must leave the previous content of a JSON output file in place.
  * ExecuteTransform clears the output sink before it writes, without forcing the clear.
  */
class TransformFailureKeepsJsonTest extends AnyFlatSpec with Matchers {

  behavior of "A transformation that fails while it generates the entities for its JSON output"

  private val previousOutput = """[{"previous": "output"}]"""

  it should "keep the previous content of the output file" in {
    val resources = InMemoryResourceManager()

    // Input dataset: two root entities
    val inputResource = resources.get("transformFailureInput.json")
    inputResource.writeString("""[{"id": 0}, {"id": 1}]""")
    val inputTask = PlainTask("input", DatasetSpec(JsonDataset(file = inputResource)))

    val outputResource = resources.get("transformFailureOutput.json")
    outputResource.writeString(previousOutput)
    val outputDataset = JsonDataset(file = outputResource)

    // The root target allows a single entity, so the transformation fails at the second root entity
    val rule = RootMappingRule(
      mappingTarget = MappingTarget(propertyUri = "", valueType = ValueType.URI, isAttribute = true),
      rules =
        MappingRules(
          propertyRules = Seq(
            DirectMapping(
              id = "rootId", sourcePath = UntypedPath("id"), mappingTarget = MappingTarget(propertyUri = Uri("id"),
                valueType = ValueType.INT, isAttribute = true)
            )
          )
        )
    )

    val transformTask = PlainTask(
      "transformTask", TransformSpec(selection = DatasetSelection(inputId = "test"), mappingRule = rule)
    )
    val execute = new ExecuteTransform(
      task = transformTask,
      inputTask = _ => Some(inputTask),
      input = user => ExecutorRegistry.access(inputTask).source(user),
      output = user => ExecutorRegistry.access(outputDataset).entitySink(user),
      pluginContext = _ => PluginContext.empty
    )

    intercept[MultipleValuesException] {
      Activity(execute).startBlocking()(UserContext.Empty)
    }

    outputResource.loadAsString() shouldBe previousOutput
  }
}
