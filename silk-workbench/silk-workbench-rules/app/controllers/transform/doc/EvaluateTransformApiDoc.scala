package controllers.transform.doc

object EvaluateTransformApiDoc {

  final val evaluateRuleRequestExample =
    """
       {
        "type" : "complex",
        "id" : "label",
        "operator" : {
          "type" : "transformInput",
          "id" : "lowerCase",
          "function" : "lowerCase",
          "inputs" : [ {
            "type" : "pathInput",
            "id" : "label",
            "path" : "loanState"
          } ],
          "parameters" : { }
        },
        "mappingTarget" : {
          "uri" : "loanState",
          "valueType" : {
            "nodeType" : "StringValueType"
          },
          "isBackwardProperty" : false,
          "isAttribute" : false
        }
      }
    """

  final val evaluateRuleResponseExample =
    """
      [
        {
          "operatorId": "lowerCase",
          "values": [
             "arizona"
          ],
          "error": null,
          "children": [
            {
              "operatorId": "label",
              "values": [
                "Arizona"
              ],
              "error": null
            }
          ]
        },
        {
          "operatorId": "lowerCase",
          "values": [
            "georgia"
          ],
          "error": null,
          "children": [
            {
              "operatorId": "label",
              "values": [
                "Georgia"
              ],
              "error": null
            }
          ]
        }
     ]
    """

  final val evaluateRuleResponseExampleTODO =
    """[]"""
}
