package controllers.transform.doc

object AutoCompletionApiDoc {

  final val pathCompletionExample =
    """
      [
        {
          "value": "source:age (The value to be inserted into the textbox, if the user selects this suggestion)",
          "label": "age (Human-readable label. Never null, will be generated if not available)",
          "description": "May be null, if not available",
          "category": "Source Paths (Results should be grouped in categories)"
        },
        {
          "value": "source:name",
          "label": "name",
          "description": "Some description",
          "category": "Source Paths"
        },
        {
          "value": "foaf:",
          "label": "foaf:",
          "description": null,
          "category": "Prefixes"
        }
      ]
    """

  final val partialSourcePathsRequestExample =
    """
      {
        "inputString": "pathPart1/pathPart2[valueA < 5]/valueB",
        "cursorPosition": 33,
        "maxSuggestions": 50
      }
    """

  final val partialSourcePathsResponseExample =
    """
      {
        "inputString": "pathPart1/pathPart2[valueA < 5]/valueB",
        "cursorPosition": 33,
        "replacementResults": [
          {
            "replacementInterval": {
              "from": 32,
              "length": 6
            },
            "extractedQuery": "valueB",
            "replacements": [
              {
                "value": "valueX",
                "label": "Value X",
                "description": "Description of value X"
              },
              {
                "value": "valueY",
                "label": "Value Y",
                "description": "Description of value Y"
              }
            ]
          },
          {
            "extractedQuery": "",
            "replacementInterval": {
              "from": 33,
              "length": 0
            },
            "replacements": [
              {
                "description": "Starts a forward path segment",
                "value": "/"
              },
              {
                "description": "Starts a backward path segment",
                "value": "\\"
              },
              {
                "description": "Starts a property filter expression",
                "value": "["
              }
            ]
          }
        ]
      }
    """


  final val partialSourcePathsResponseDescription =
    """
The response contains the original parameters of the request for the client to check to which request it
belongs if it fired multiple requests.
There can be multiple (or none) suggestion result sections under `replacementResults` that differ in what part
of the original string gets replaced.
The `replacementInterval` specifies the sub-string
of the input path string that can be replaced by the proposed partial path suggestions. It is defined
by the start index and length of the substring that should be replaced.
The `extractedQuery` property is the query that was extracted from the original input to seed the auto-completion
request. This can be empty if the result is not filtered.
The `replacements` contain the suggestions that should be displayed to the user that the specified
substring can be replaced with. The `value` is the actual value of the suggestion and the optional `label`
and `description` values can be used for a more human-readable version of the value.
    """

  final val valueTypesExample =
    """
      [
        {
          "value": "FloatValueType",
          "label": "Float",
          "description": "Numeric values which can have a fractional value, represented as IEEE single-precision 32-bit floating point numbers. Examples for valid values are: '1.9'. Invalid values are: '1,9'.",
          "category": "Uncategorized",
          "isCompletion": true
        },
        {
          "value": "IntValueType",
          "label": "Int",
          "description": "Numeric values without a fractional component, represented as 32-bit signed integers. Numbers must be between -2147483648 and 2147483647. Examples for valid values are: '1'. Invalid values are: '1.0', '1234567890123456789012345678901234567890'.",
          "category": "Uncategorized",
          "isCompletion": true
        },
        "..."
      ]
    """
}
