package controllers.transform.autoCompletion

import org.scalatest.{FlatSpec, MustMatchers}

class PartialSourcePathAutoCompletionRequestTest extends FlatSpec with MustMatchers {
  behavior of "partial source path auto-completion request"

  it should "compute the index of the last path operator before the cursor position correctly" in {
    operatorPositionBeforeCursor("some:uri/", 9) mustBe Some(8)
    operatorPositionBeforeCursor("""a/b[c = "value"]""", 12) mustBe Some(3)
  }

  private def operatorPositionBeforeCursor(inputString: String, cursorPosition: Int): Option[Int] = {
    PartialSourcePathAutoCompletionRequest(inputString, cursorPosition, None).pathOperatorIdxBeforeCursor
  }
}
