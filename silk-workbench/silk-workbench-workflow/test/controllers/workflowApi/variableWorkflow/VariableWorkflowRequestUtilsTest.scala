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

  it should "parse execution variables from reserved query parameters" in {
    implicit val request = FakeRequest("GET", "/workflow?variable-greeting=World&inputProp=Hello")
    val variables = VariableWorkflowRequestUtils.parseExecutionVariables
    variables.variables.map(_.name) shouldBe Seq("greeting")
    variables.map("greeting").value shouldBe "World"
  }

  it should "merge execution variables from query parameters and the JSON body key" in {
    implicit val request = FakeRequest("POST", "/workflow?variable-other=fromQuery").withBody(AnyContentAsJson(entityWithReservedKey))
    val variables = VariableWorkflowRequestUtils.parseExecutionVariables
    variables.map("greeting").value shouldBe "World"
    variables.map("other").value shouldBe "fromQuery"
  }

  it should "reject a variable that is defined both in the JSON body key and as a query parameter" in {
    implicit val request = FakeRequest("POST", "/workflow?variable-greeting=fromQuery").withBody(AnyContentAsJson(entityWithReservedKey))
    a[BadUserInputException] should be thrownBy VariableWorkflowRequestUtils.parseExecutionVariables
  }

  it should "reject a reserved query parameter that is repeated with different values" in {
    implicit val request = FakeRequest("GET", "/workflow?variable-greeting=World&variable-greeting=Mars")
    a[BadUserInputException] should be thrownBy VariableWorkflowRequestUtils.parseExecutionVariables
  }

  it should "accept a reserved query parameter that is repeated with the same value" in {
    implicit val request = FakeRequest("GET", "/workflow?variable-greeting=World&variable-greeting=World")
    VariableWorkflowRequestUtils.parseExecutionVariables.map("greeting").value shouldBe "World"
  }

  it should "not make reserved query parameters part of the query-string input entity" in {
    implicit val request = FakeRequest("GET", "/workflow?variable-greeting=World&inputProp=Hello")
    VariableWorkflowRequestUtils.requestToInputResource(None) shouldBe Some(Json.obj("inputProp" -> Json.arr("Hello")))
  }

  it should "reject a request whose query string contains only reserved parameters and no input entity properties" in {
    implicit val request = FakeRequest("GET", "/workflow?variable-greeting=World")
    a[BadUserInputException] should be thrownBy VariableWorkflowRequestUtils.requestToInputResource(None)
  }
}
