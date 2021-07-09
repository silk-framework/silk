package controllers.workspaceApi.doc

object ActivitiesApiDoc {

  final val activitiesExample =
    """
      [
          {
              "activity": "TypesCache",
              "cancelled": false,
              "concreteStatus": "Successful",
              "exceptionMessage": null,
              "failed": false,
              "isRunning": false,
              "lastUpdateTime": 1595861385992,
              "message": "Finished in 46ms",
              "progress": 100,
              "project": "singleProject",
              "runtime": 46,
              "startTime": 1595861385946,
              "statusName": "Finished",
              "task": "d57c393f-8f3f-48ba-ba13-8e815e04d557_CSVdataset"
          },
          {
              "activity": "ExecuteTransform",
              "concreteStatus": "Not executed",
              "failed": false,
              "isRunning": false,
              "lastUpdateTime": 1595861385941,
              "message": "Idle",
              "progress": null,
              "project": "singleProject",
              "startTime": null,
              "statusName": "Idle",
              "task": "a0d18ae0-085b-4a06-aee1-b4c19bd00eac_failTransform"
          },
          {
              "activity": "ExecuteLocalWorkflow",
              "cancelled": false,
              "concreteStatus": "Failed",
              "exceptionMessage": "Exception during execution of workflow operator a0d18ae0-085b-4a06-aee1-b4c19bd00eac_failTransform. Cause: No input given to transform specification executor a0d18ae0-085b-4a06-aee1-b4c19bd00eac_failTransform!",
              "failed": true,
              "isRunning": false,
              "lastUpdateTime": 1595861468748,
              "message": "Failed after 135ms: Exception during execution of workflow operator a0d18ae0-085b-4a06-aee1-b4c19bd00eac_failTransform. Cause: No input given to transform specification executor a0d18ae0-085b-4a06-aee1-b4c19bd00eac_failTransform!",
              "progress": 100,
              "project": "singleProject",
              "runtime": 135,
              "startTime": 1595861468613,
              "statusName": "Finished",
              "task": "e7dc14e5-b45b-4dc5-9933-bbc2750630f5_failedWorkflow"
          }
      ]
    """

}
