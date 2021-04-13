package controllers.transform.autoCompletion

import org.scalatest.{FlatSpec, MustMatchers}
import org.silkframework.config.Prefixes

class PartialSourcePathAutocompletionHelperTest extends FlatSpec with MustMatchers {
  behavior of "PartialSourcePathAutocompletionHelper"

  def replace(inputString: String,
              cursorPosition: Int,
              openWorld: Boolean = false): PathToReplace = {
    PartialSourcePathAutocompletionHelper.pathToReplace(PartialSourcePathAutoCompletionRequest(inputString, cursorPosition, None), openWorld)(Prefixes.empty)
  }

  it should "correctly find out which part of a path to replace" in {
    val input = "a1/b1/c1"
    replace(input, 4) mustBe PathToReplace(2, 3, Some(Seq("b1")))
    replace(input, 5) mustBe PathToReplace(2, 3, Some(Seq("b1")))
  }
}
