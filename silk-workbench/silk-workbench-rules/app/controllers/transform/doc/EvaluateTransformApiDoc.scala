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

  final val evaluatedRuleResponseExample =
    """
{
    "rules": [
        {
            "type": "complex",
            "id": "uri",
            "operator": {
                "type": "transformInput",
                "id": "buildUri",
                "function": "concat",
                "inputs": [
                    {
                        "type": "transformInput",
                        "id": "constant0",
                        "function": "constant",
                        "inputs": [],
                        "parameters": {
                            "value": "http://domain.com/person/"
                        }
                    },
                    {
                        "type": "transformInput",
                        "id": "encode1",
                        "function": "urlEncode",
                        "inputs": [
                            {
                                "type": "pathInput",
                                "id": "path1",
                                "path": "ID"
                            }
                        ],
                        "parameters": {
                            "encoding": "UTF-8"
                        }
                    }
                ],
                "parameters": {
                    "glue": "",
                    "missingValuesAsEmptyStrings": "false"
                }
            },
            "sourcePaths": [
                "ID"
            ]
        },
        {
            "type": "object",
            "id": "object",
            "sourcePath": "Properties/Property",
            "mappingTarget": {
                "uri": "<https://www.eccemca.com/direct/property>",
                "valueType": {
                    "nodeType": "UriValueType"
                },
                "isBackwardProperty": false,
                "isAttribute": false
            },
            "rules": {
                "uriRule": null,
                "typeRules": [],
                "propertyRules": [
                    {
                        "type": "complex",
                        "id": "URI",
                        "operator": {
                            "type": "transformInput",
                            "id": "buildUri",
                            "function": "concat",
                            "inputs": [
                                {
                                    "type": "transformInput",
                                    "id": "fixUri0",
                                    "function": "uriFix",
                                    "inputs": [
                                        {
                                            "type": "pathInput",
                                            "id": "path0",
                                            "path": ""
                                        }
                                    ],
                                    "parameters": {
                                        "uriPrefix": "urn:url-encoded-value:"
                                    }
                                },
                                {
                                    "type": "transformInput",
                                    "id": "constant1",
                                    "function": "constant",
                                    "inputs": [],
                                    "parameters": {
                                        "value": "/object"
                                    }
                                }
                            ],
                            "parameters": {
                                "glue": "",
                                "missingValuesAsEmptyStrings": "false"
                            }
                        },
                        "sourcePaths": [],
                        "metadata": {},
                        "layout": {
                            "nodePositions": {}
                        },
                        "uiAnnotations": {
                            "stickyNotes": []
                        }
                    }
                ]
            },
            "metadata": {}
        }
    ],
    "evaluatedEntities": [
        {
            "uris": [
                "http://domain.com/person/1"
            ],
            "values": [
                {
                    "operatorId": "buildUri",
                    "values": [
                        "http://domain.com/person/1"
                    ],
                    "error": null,
                    "children": [
                        {
                            "operatorId": "constant0",
                            "values": [
                                "http://domain.com/person/"
                            ],
                            "error": null,
                            "children": []
                        },
                        {
                            "operatorId": "encode1",
                            "values": [
                                "1"
                            ],
                            "error": null,
                            "children": [
                                {
                                    "operatorId": "path1",
                                    "values": [
                                        "1"
                                    ],
                                    "error": null
                                }
                            ]
                        }
                    ]
                },
                {
                    "operatorId": "buildUri",
                    "values": [
                        "urn:instance:Property#10-7/object",
                        "urn:instance:Property#14-7/object",
                        "urn:instance:Property#18-7/object"
                    ],
                    "error": null,
                    "children": [
                        {
                            "operatorId": "fixUri0",
                            "values": [
                                "urn:instance:Property#10-7",
                                "urn:instance:Property#14-7",
                                "urn:instance:Property#18-7"
                            ],
                            "error": null,
                            "children": [
                                {
                                    "operatorId": "path0",
                                    "values": [
                                        "urn:instance:Property#10-7",
                                        "urn:instance:Property#14-7",
                                        "urn:instance:Property#18-7"
                                    ],
                                    "error": null
                                }
                            ]
                        },
                        {
                            "operatorId": "constant1",
                            "values": [
                                "/object"
                            ],
                            "error": null,
                            "children": []
                        }
                    ]
                }
            ]
        }
    ]
}
      """
}
