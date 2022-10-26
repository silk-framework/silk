package org.silkframework.rule

import org.scalatest.{FlatSpec, Matchers}
import org.silkframework.config.{PlainTask, Prefixes}
import org.silkframework.entity.ValueType
import org.silkframework.entity.paths.UntypedPath
import org.silkframework.execution.AbortExecutionException
import org.silkframework.plugins.dataset.json.JsonDataset
import org.silkframework.rule.execution.ExecuteTransform
import org.silkframework.rule.execution.local.MultipleValuesException
import org.silkframework.runtime.activity.{Activity, UserContext}
import org.silkframework.runtime.resource.InMemoryResourceManager
import org.silkframework.util.Uri
import play.api.libs.json.{JsArray, JsNumber, JsValue, Json}

class TransformSingleEntityFlagTest extends FlatSpec with Matchers {

  behavior of "TransformedEntities"

  //TODO
  ignore should "check if multiple root entities are not allowed" in {
    executeTransform(
      generateJson(rootCount = 1, childCount = 1, valueCount = 1),
      generateRule(singleRoot = true, singleChild = true, singleChildValue = true),
      expectFailure = false
    )
    executeTransform(
      generateJson(rootCount = 2, childCount = 1, valueCount = 1),
      generateRule(singleRoot = true, singleChild = true, singleChildValue = true),
      expectFailure = true
    )
  }

  it should "check if multiple child entities are not allowed" in {
    executeTransform(
      generateJson(rootCount = 1, childCount = 2, valueCount = 1),
      generateRule(singleRoot = true, singleChild = true, singleChildValue = true),
      expectFailure = true
    )
    executeTransform(
      generateJson(rootCount = 1, childCount = 1, valueCount = 1),
      generateRule(singleRoot = true, singleChild = true, singleChildValue = true),
      expectFailure = false
    )
    executeTransform(
      generateJson(rootCount = 1, childCount = 2, valueCount = 1),
      generateRule(singleRoot = true, singleChild = false, singleChildValue = true),
      expectFailure = false
    )
  }

  /**
    * Executes a rule and makes sure that the output equals the provided input.
    */
  private def executeTransform(inputJson: JsValue, rule: RootMappingRule, expectFailure: Boolean): Unit = {
    implicit val prefixes: Prefixes = Prefixes.empty
    val resources = InMemoryResourceManager()

    // Input dataset
    val inputResource = resources.get("input.json")
    inputResource.writeString(Json.stringify(inputJson))
    val inputDataset = JsonDataset(file = inputResource)

    // Output dataset
    val outputResource = resources.get("output.json")
    val outputDataset = JsonDataset(file = outputResource)

    // Execute transform
    val transformTask = PlainTask("transformTask", TransformSpec(selection = DatasetSelection(inputId = "test"), mappingRule = rule, abortIfErrorsOccur = true))
    val execute = new ExecuteTransform(transformTask, user => inputDataset.source(user), user => outputDataset.entitySink(user))

    try {
      Activity(execute).startBlocking()(UserContext.Empty)
    } catch {
      case ex @ AbortExecutionException(_, Some(_: MultipleValuesException)) if expectFailure =>
        return
    }

    if(expectFailure) {
      fail(s"Expected to fail for input $inputJson and rule $rule")
    }

    // Test if output matches input
    val outputJson = outputResource.read(Json.parse)
    outputJson shouldBe inputJson
  }

  /**
    * Generates a nested JSON.
    *
    * @param rootCount Number of root entities.
    * @param childCount Number of children per root entity.
    * @param valueCount Number of values per child.
    */
  private def generateJson(rootCount: Int, childCount: Int, valueCount: Int): JsValue = {
    val values = {
      if(valueCount == 1) {
        JsNumber(0)
      } else {
        JsArray(IndexedSeq.tabulate(valueCount)(i => JsNumber(i)))
      }
    }

    val children =
      if(childCount == 1) {
        Json.obj("id" -> JsNumber(0), "values" -> values)
      } else {
        JsArray(IndexedSeq.tabulate(childCount)(i => Json.obj("id" -> JsNumber(i), "values" -> values)))
      }

    val rootEntities =
      if (rootCount == 1) {
        Json.obj("id" -> JsNumber(0), "children" -> children)
      } else {
        JsArray(IndexedSeq.tabulate(rootCount)(i => Json.obj("id" -> JsNumber(i), "children" -> children)))
      }

    rootEntities
  }

  /**
    * Generates a transform rule for testing.
    *
    * @param singleRoot Only allow a single root entity.
    * @param singleChild Only allow a single child per entity.
    * @param singleChildValue Only allow a single value per child entity.
    * @return
    */
  private def generateRule(singleRoot: Boolean, singleChild: Boolean, singleChildValue: Boolean) = {
    RootMappingRule(
      mappingTarget = MappingTarget(propertyUri = "", valueType = ValueType.URI, isAttribute = singleRoot),
      rules =
        MappingRules(
          propertyRules = Seq(
            DirectMapping(id = "rootId", sourcePath = UntypedPath("id"), mappingTarget = MappingTarget(propertyUri = Uri("id"), valueType = ValueType.INT, isAttribute = true)),
            ObjectMapping(
              sourcePath = UntypedPath("children"),
              target = Some(MappingTarget(propertyUri = Uri("children"), valueType = ValueType.URI, isAttribute = singleChild)),
              rules =
                MappingRules(
                  propertyRules = Seq(
                    DirectMapping(id = "childId", sourcePath = UntypedPath("id"), mappingTarget = MappingTarget(propertyUri = Uri("id"), valueType = ValueType.INT, isAttribute = true)),
                    DirectMapping(sourcePath = UntypedPath("values"), mappingTarget = MappingTarget(propertyUri = Uri("values"), valueType = ValueType.INT, isAttribute = singleChildValue))
                  )
                )
            )
          )
        )
    )
  }

}
