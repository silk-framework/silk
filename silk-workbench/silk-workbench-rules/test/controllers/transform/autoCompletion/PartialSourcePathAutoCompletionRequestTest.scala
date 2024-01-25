package controllers.transform.autoCompletion

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class PartialSourcePathAutoCompletionRequestTest extends AnyFlatSpec with Matchers {
  behavior of "partial source path auto-completion request"

  it should "compute the index of the last path operator before the cursor position correctly" in {
    operatorPositionBeforeCursor("some:uri/", 9) mustBe Some(8)
    operatorPositionBeforeCursor("""a/b[c = "value"]""", 12) mustBe Some(3)
  }

  it should "know if the cursor is placed in a backward operator" in {
    isInBackwardOp("\\") mustBe true
    isInBackwardOp("\\<urn:test:end>") mustBe true
    isInBackwardOp("\\some query text") mustBe true
    isInBackwardOp("firstOp\\some query text") mustBe true
    isInBackwardOp("/firstOp\\some query text") mustBe true
    isInBackwardOp("\\path/forward") mustBe false
    isInBackwardOp("\\path[propertyFilter") mustBe false
  }

  it should "know if the cursor is placed in an explicit forward operator" in {
    isInExplicitForwardOp("") mustBe false
    isInExplicitForwardOp("/") mustBe true
    isInExplicitForwardOp("/some query") mustBe true
    isInExplicitForwardOp("\\backward/forward") mustBe true
    isInExplicitForwardOp("/path[filter") mustBe false
    isInExplicitForwardOp("/forward\\backward") mustBe false
  }

  private def isInBackwardOp(path: String): Boolean = {
    PartialSourcePathAutoCompletionRequest(path, path.length, None, None, None).isInBackwardOp
  }

  private def isInExplicitForwardOp(path: String): Boolean = {
    PartialSourcePathAutoCompletionRequest(path, path.length, None, None, None).isInExplicitForwardOp
  }

  private def operatorPositionBeforeCursor(inputString: String, cursorPosition: Int): Option[Int] = {
    PartialSourcePathAutoCompletionRequest(inputString, cursorPosition, None, None, None).pathOperatorIdxBeforeCursor
  }
}
