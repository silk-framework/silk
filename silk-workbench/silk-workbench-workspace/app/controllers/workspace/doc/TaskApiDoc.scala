package controllers.workspace.doc

object TaskApiDoc {

  final val taskExampleJson =
    """
      {
        "id": "myTransform",
        "metadata": {
          "label": "task label",
          "description": "task description"
        },
        "data": {
          "taskType": "Transform",
          "selection": {
            "inputId": "DBpedia",
            "typeUri": "http://dbpedia.org/ontology/Film",
            "restriction": ""
          },
          "outputs": [],
          "targetVocabularies": []
        }
      }
    """

  final val taskExampleWithoutLabelsJson =
    """
      {
        "id": "transform",
        "metadata": {
          "label": "task label",
          "description": "task description"
        },
        "data": {
          "taskType": "Transform",
          "parameters": {
            "selection": {
              "inputId": "DBpedia",
              "typeUri": "http://dbpedia.org/ontology/Film",
              "restriction": ""
            },
            "mappingRule": {
              "type": "root",
              "id": "root",
              "rules": {
                "uriRule": null,
                "typeRules": [],
                "propertyRules": []
              }
            },
            "outputs": [],
            "targetVocabularies": []
          }
        }
      }
    """

  final val taskMetadataExampleWithLabelsJson =
    """
      {
        "data": {
            "parameters": {
                "mappingRule": {
                    "value": {
                        "id": "root",
                        "rules": { },
                        "type": "root"
                    }
                },
                "output": {
                    "value": ""
                },
                "selection": {
                    "value": {
                        "inputId": {
                            "label": "Some labe",
                            "value": "datasetresource_1499719467735_loans_csv"
                        },
                        "restriction": {
                            "value": ""
                        },
                        "typeUri": {
                            "value": ""
                        }
                    }
                },
                "targetVocabularies": {
                    "value": []
                }
            },
            "taskType": "Transform"
        },
        "id": "transform_datasetresource_1499719467735_loans_csv",
        "metadata": {
            "label": "",
            "modified": "2020-04-07T11:05:59.574Z"
        },
        "project": "cmem",
        "taskType": "Transform"
      }
    """

  final val taskMetadataExampleJson =
    """
      {
        "label": "Task Label",
        "description": "Task Description",
        "modified": "2018-05-24T14:45:42.637Z",
        "project": "MyProject",
        "id": "MyTask",
        "taskType": "Dataset"
        "schemata": {
          "input": [{
            "paths": [
              "<http://xmlns.com/foaf/0.1/name>",
              "<http://dbpedia.org/ontology/releaseDate>"
            ]
          }],
          "output": {
            "paths": [
              "targetUri",
              "confidence"
            ]
          }
        },
        "relations": {
          "inputTasks": [],
          "outputTasks": [],
          "referencedTasks": ["DBpedia", "linkedmdb"],
          "dependentTasksDirect": [{id: "workflow_f76889ec", label: "Workflow label", taskLink: "/workbench/projects/MyProject/workflow/MyTask"}],
          "dependentTasksAll": [{id: "workflow_f76889ec", label: "Workflow label", taskLink: "/workbench/projects/MyProject/workflow/MyTask"}]
        }
      }
    """

  final val copyTaskRequestJsonExample =
    """
      {
        "targetProject": "targetProjectId",
        "dryRun": true,
        "overwriteTasks": true
      }
    """

  final val copyTaskResponseJsonExample =
    """
    {
      "copiedTasks": [
        {
          "taskType": "Dataset",
          "id": "linkedmdb",
          "label": "linkedmdb",
          "originalTaskLink": "/workbench/projects/json/dataset/linkedmdb"
        },
        {
          "taskType": "Linking",
          "id": "movies",
          "label": "movies",
          "originalTaskLink": "/workbench/projects/json/linking/movies"
        }
      ],
      "overwrittenTasks": [
        {
          "taskType": "Dataset",
          "id": "DBpedia",
          "label": "DBpedia",
          "originalTaskLink": "/workbench/projects/json/dataset/DBpedia",
          "overwrittenTaskLink": "/workbench/projects/movies/dataset/DBpedia"
        }
      ]
    }
  """

  final val taskContextRequest =
    """
    {
        "taskContext": {
            "inputTasks": [
                {
                    "id": "XML_d0c8c145ce21fa50"
                }
            ],
            "outputTasks": [
                {
                    "id": "dataset_457989759278592",
                    "configPort": false,
                    "inputPort": 0
                },
                {
                    "id": "customOperator_9d8da649976a4c2b",
                    "configPort": true
                }
            ]
        }
    }
    """

  final val taskContextResponse =
    """
    {
        "inputTasks": [
            {
                "taskId": "XML_d0c8c145ce21fa50",
                "label": "XML dataset",
                "isDataset": true,
                "fixedSchema": false
            }
        ],
        "outputTasks": [
            {
                "taskId": "dataset_457989759278592",
                "label": "Some dataset",
                "isDataset": true,
                "fixedSchema": false
            },
            {
                "taskId": "customOperator_9d8da649976a4c2b",
                "label": "Custom operator",
                "isDataset": false,
                "fixedSchema": true
            }
        ]
    }
    """
}
