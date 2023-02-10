package controllers.workspaceApi.coreApi.doc

object VariableTemplateApiDoc {
  final val autoCompleteVariableTemplateRequest =
    """
      {
        "inputString": "{{",
        "cursorPosition": 2,
        "maxSuggestions": 50
      }
    """

  final val validateVariableTemplateRequest =
    """
      {
        "templateString": "{{var}}"
      }
    """
}
