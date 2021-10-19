package controllers.workspace.doc

object SearchApiDoc {

  final val searchTasksRequestExample =
    """
      {
        "project": null
        "searchTerm": null
        "formatOptions": {
          "includeMetaData": true,
          "includeTaskData": true,
          "includeTaskProperties": false,
          "includeRelations": false,
          "includeSchemata": false
        }
      }
    """

  final val searchTasksResponseExample =
    """
      [
        {
          "id": "task identifier (unique inside the project)",
          "project": "project this task belongs to (if any)",
          "metadata": {
            "label": "task label",
            "description": "task description",
            "modified": "2018-03-08T11:04:59.156Z"
          },
          "taskType": "task type, e.g., Dataset",
          "data": {
            "task data": "will be here"
          },
          "properties": [
            {
              "key": "some key",
              "value": "some value"
            }
          ],
          "relations": {
            "inputTasks": [ "Identifiers of all tasks from which this task is reading data." ],
            "outputTasks": [ "Identifiers of all tasks to which this task is writing data." ],
            "referencedTasks": [ "Identifiers of all tasks that are directly referenced by this task. Includes input and output tasks." ],
            "dependentTasksDirect": [ "Identifiers of all tasks that directly reference this task." ],
            "dependentTasksAll": [ "Identifiers of all tasks that directly or indirectly reference this task." ]
          },
          "schemata": {
            "input": "The schemata of the input data of this task.",
            "output": "The schemata of the output data of this task."
          }
        }
      ]
    """

  final val artifactSearchRequestExample =
    """
      {
        "project": "cmem",
        "itemType": "dataset",
        "textQuery": "production database",
        "offset": 0,
        "limit": 20,
        "sortBy": "label",
        "sortOrder": "ASC",
        "addTaskParameters": false,
        "facets": [
          {
            "facetId": "datasetType",
            "type": "keyword",
            "keywordIds": ["csv", "eccencaDataPlatform"]
          }
        ]
      }
    """

  final val artifactSearchResponseExample =
    """
      {
        "total": 110,
        "results": [
          {
            "type": "Project",
            "id": "cmem",
            "label": "CMEM",
            "description": "CMEM project",
            "itemLinks": []
          },
          {
            "type": "Dataset",
            "project": "cmem",
            "projectLabel": "CMEM the project",
            "pluginId": "csv",
            "pluginLabel": "CSV",
            "id": "customers",
            "label": "Customers",
            "description": "Customers dataset"
          },
          {
            "type": "Task",
            "pluginId": "script",
            "pluginLabel": "Script",
            "projectLabel": "CMEM the project",
            "project": "cmem",
            "id": "processX",
            "label": "Process X",
            "description": "Process X via script",
            "itemLinks": []
          },
          {
            "description": "",
            "id": "transform_a_to_b",
            "pluginId": "transform",
            "pluginLabel": "Tramsformation",
            "itemLinks": [
              {
                "label": "Mapping editor",
                "path": "/transform/pmd/transform_a_to_b/editor"
              },
              {
                "label": "Transform evaluation",
                "path": "/transform/pmd/transform_a_to_b/evaluate"
              },
              {
                "label": "Transform execution",
                "path": "/transform/pmd/transform_a_to_b/execute"
              }
            ],
            "label": "Transform A to B",
            "projectId": "cmem",
            "type": "Transform"
          }
        ],
        "sortByProperties": [
          {
            "id": "label",
            "label": "Label"
          }
        ],
        "facets": [
          {
            "id": "tag",
            "label": "Tag",
            "description": "A user supplied tag for custom categorization.",
            "type": "keyword",
            "values": [
              {
                "id": "test",
                "label": "Test",
                "count": 2
              },
              {
                "id": "public",
                "label": "Public",
                "count": 3
              },
              {
                "id": "private",
                "label": "Private",
                "count": 4
              }
            ]
          },
          {
            "id": "datasetType",
            "label": "Dataset type",
            "description": "The concrete type of a dataset, which comprises its format and other characteristics.",
            "type": "keyword",
            "values": [
              {
                "count": 43,
                "id": "eccencaDataPlatform",
                "label": "Knowledge Graph"
              },
              {
                "count": 17,
                "id": "csv",
                "label": "CSV"
              }
            ]
          }
        ]
      }
    """

  final val itemTypesExample =
    """
      {
        "label": "Type",
        "values": [{"id": "project", "label": "Project"}, {"id": "dataset", "label": "Dataset"}, {"id": "transformation", "label": "Transformation"}, {"id": "linking", "label": "Linking"}, {"id": "workflow", "label": "Workflow"}, {"id": "task", "label": "Task"}]
      }
    """

  final val recentlyViewedItemsExample =
    """
      [
        {
          "itemLinks": [
            {
              "label": "Task details page",
              "path": "/workbench/projects/multiInputRestProject/task/parseJSON"
            }
          ],
          "itemType": "task",
          "pluginId": "JsonParserOperator",
          "pluginLabel": "Parse JSON",
          "projectId": "multiInputRestProject",
          "projectLabel": "Multiple Inputs REST project",
          "taskId": "parseJSON",
          "taskLabel": "This will be updated"
        },
        {
          "itemLinks": [
            {
              "label": "Project details page",
              "path": "/workbench/projects/multiInputRestProject"
            }
          ],
          "itemType": "project",
          "projectId": "multiInputRestProject",
          "projectLabel": "Multiple Inputs REST project"
        }
      ]
    """

  final val parameterAutoCompletionRequestExample =
    """
      {
        "pluginId": "Scheduler",
        "parameterId": "task",
        "projectId": "cmem",
        "dependsOnParameterValues": ["value the auto-completion depends on"],
        "textQuery": "sched",
        "offset": 0,
        "limit": 10
      }
    """

  final val parameterAutoCompletionResponseExample =
    """
      [
        {
          "label": "Scheduled workflow 1",
          "value": "workflow1"
        },
        {
          "value": "workflow2"
        }
      ]
    """
}
