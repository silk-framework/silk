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
}
