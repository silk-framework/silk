package controllers.workflowApi.variableWorkflow

import controllers.workflowApi.variableWorkflow.VariableWorkflowRequestUtils.EXECUTION_VARIABLES_KEY
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.silkframework.runtime.validation.BadUserInputException
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsJson
import play.api.test.FakeRequest

class VariableWorkflowRequestUtilsTest extends AnyFlatSpec with Matchers {

  behavior of "VariableWorkflowRequestUtils"

  private val entityWithReservedKey = Json.obj(
    "value" -> "Hello",
    EXECUTION_VARIABLES_KEY -> Json.obj("greeting" -> "World")
  )

  it should "parse execution variables from the reserved key of a JSON body" in {
    implicit val request = FakeRequest().withBody(AnyContentAsJson(entityWithReservedKey))
    val variables = VariableWorkflowRequestUtils.parseExecutionVariables
    variables.map("greeting").value shouldBe "World"
  }

  it should "not make the reserved execution variables key part of the input entity" in {
    implicit val request = FakeRequest().withBody(AnyContentAsJson(entityWithReservedKey))
    VariableWorkflowRequestUtils.requestToInputResource(Some("application/json")) shouldBe Some(Json.obj("value" -> "Hello"))
  }

  it should "reject a reserved key that does not hold a name/value map" in {
    implicit val request = FakeRequest().withBody(AnyContentAsJson(Json.obj("value" -> "Hello", EXECUTION_VARIABLES_KEY -> "notAMap")))
    a[BadUserInputException] should be thrownBy VariableWorkflowRequestUtils.parseExecutionVariables
  }

  it should "return no execution variables when the reserved key is absent" in {
    implicit val request = FakeRequest().withBody(AnyContentAsJson(Json.obj("value" -> "Hello")))
    VariableWorkflowRequestUtils.parseExecutionVariables.variables shouldBe empty
  }
}
