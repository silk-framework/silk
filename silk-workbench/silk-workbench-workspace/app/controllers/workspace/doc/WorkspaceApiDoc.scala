package controllers.workspace.doc

object WorkspaceApiDoc {

  final val projectListExample =
    """
      [
        {
          "name": "example_project",
          "tasks": {
            "dataset": [
              "dataset1",
              "dataset2"
            ],
            "transform": [
              "transformation1"
            ],
            "linking": [],
            "workflow": [
              "workflow1"
            ],
            "custom": []
          }
        },
        {
          "name": "lending",
          "tasks": {
            "dataset": [
              "cmem",
              "links",
              "loans_csv",
              "output_csv",
              "unemployment_csv"
            ],
            "transform": [
              "generateOutput",
              "transform_loans",
              "transform_loans_csv",
              "transform_unemployment"
            ],
            "linking": [
              "link_loans_unemployment",
              "linkingtest"
            ],
            "workflow": [
              "workflow1"
            ],
            "custom": []
          }
        }
      ]
    """

  final val projectExample =
    """
      {
        "name": "movies",
        "metaData": {
          "label": "movies"
        },
        "tasks": {
          "dataset": [
            "DBpedia",
            "linkedmdb"
          ],
          "transform": [],
          "linking": [
            "movies"
          ],
          "workflow": [],
          "custom": []
        }
      }
    """

  final val copyProjectRequest =
    """
      {
        "targetProject": "targetProjectId",
        "dryRun": true,
        "overwriteTasks": true
      }
    """

  final val copyProjectResponse =
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

}
