package controllers.workspaceApi.doc

object ReportsApiDoc {

  final val reportListExample =
    """
      [
        {
          "project": "movies",
          "task": "simpleWorkflow",
          "time": "2020-11-26T14:20:29.511Z"
        },
        {
          "project": "movies",
          "task": "simpleWorkflow",
          "time": "2020-11-26T14:22:13.462Z"
        }
      ]
    """

  final val reportExample =
    """
      {
        "metaData": {
          "startedByUser": null,
          "startedAt": "2020-11-26T14:20:28.502Z",
          "finishedAt": "2020-11-26T14:20:29.510Z",
          "cancelledAt": null,
          "cancelledBy": null,
          "finishStatus": {
            "final": "status"
          }
        },
        "value": {
          "summary": [],
          "task": {
            "some": "specification"
          },
          "warnings": [
            "Some tasks generated warnings."
          ],
          "label": "direct Workflow",
          "taskReports": {
            "DBpedia": {
              "label": "DBpedia",
              "task": {
                "some": "specification"
              },
              "summary": [],
              "warnings": []
            },
            "transform": {
              "some": "specification"
            }
          }
        }
      }
    """

  final val reportUpdatesExample =
    """
      {
        "timestamp": 1634547683161,
        "updates": [
          {
            "node": "85ad8f57-f7ba-492c-a470-9176c0d8fd4c_dataset",
            "timestamp": 1634547463671,
            "operation": "read",
            "warnings": [],
            "isDone": false,
            "entityCount": 5373
          }
        ]
      }
    """
}
