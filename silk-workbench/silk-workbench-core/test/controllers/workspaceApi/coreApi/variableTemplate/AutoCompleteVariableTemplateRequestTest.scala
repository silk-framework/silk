package controllers.workspaceApi.coreApi.variableTemplate

import controllers.autoCompletion.{AutoSuggestAutoCompletionResponse, CompletionBase, ReplacementInterval, ReplacementResults}
import org.scalatest.{FlatSpec, MustMatchers}

class AutoCompleteVariableTemplateRequestTest extends FlatSpec with MustMatchers {
  behavior of "auto-complete variable template"

  it should "return the correct suggestions" in {
    val defaultVariables = Seq("test", "someLongerVariable")
    def suggest(inputString: String, cursorPosition: Int, variables: Seq[String] = defaultVariables): AutoSuggestAutoCompletionResponse = {
      AutoCompleteVariableTemplateRequest.suggestions(AutoCompleteVariableTemplateRequest(inputString, cursorPosition, None), variables)
    }
    val s1 = suggest("{{", 2)
    s1.inputString mustBe "{{"
    s1.cursorPosition mustBe 2
    s1.replacementResults mustBe Seq(
      ReplacementResults(
        ReplacementInterval(2, 0),
        "",
        defaultVariables.map(v => CompletionBase(v))
      )
    )
    suggest("123{{te", 8).replacementResults.head mustBe ReplacementResults(
      ReplacementInterval(5, 2),
      "te",
      Seq("test").map(v => CompletionBase(v))
    )
    suggest("123{{ e var",  9).replacementResults.head mustBe ReplacementResults(
      ReplacementInterval(6, 5),
      "e var",
      Seq("someLongerVariable").map(v => CompletionBase(v))
    )
    suggest("123{{ e var}}{#comment", 21).replacementResults.head.extractedQuery mustBe ""
  }
}
