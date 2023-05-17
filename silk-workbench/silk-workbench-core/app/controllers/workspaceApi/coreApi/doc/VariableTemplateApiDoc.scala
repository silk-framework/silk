package controllers.workspaceApi.coreApi.doc

object VariableTemplateApiDoc {
  final val autoCompleteVariableTemplateRequest =
    """
    {
        "inputString": "{{",
        "cursorPosition": 2,
        "maxSuggestions": 50,
        "project": "movies"
    }
    """

  final val autoCompleteVariableTemplateResponse =
    """
    {
        "inputString": "endpointXYZ{# some comment #}",
        "cursorPosition": 19,
        "replacementResults": [
            {
                "replacementInterval": {
                    "from": 19,
                    "length": 0
                },
                "extractedQuery": "",
                "replacements": [
                    {
                        "value": "test"
                    },
                    {
                        "value": "testInt"
                    }
                ]
            }
        ]
    }
      """

  final val validateVariableTemplateRequest =
    """
      {
        "templateString": "{{var}}",
        "project": "movies"
      }
    """

  final val validateVariableTemplateResponse =
    """
    {
        "valid": false,
        "parseError": {
            "message": "Error parsing '#{does not exist}': syntax error at position 7, encountered 'not', expected '}'",
            "start": 0,
            "end": 18
        }
    }
      """
}
