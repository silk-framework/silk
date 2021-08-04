package controllers.workspace.doc

object ActivityApiDoc {

  final val activityDoc =
    """Manage activities. An activity is a unit of work that can be executed in the background. The are global activities as well as activities that belong to a task or project."""

  final val activityListExample =
    """
      [
        {
          "instances": [
            {
              "id": "GlobalVocabularyCache"
            }
          ],
          "name": "GlobalVocabularyCache"
        }
      ]
    """

  final val activityStatusDescription =
    """Retrieves the status of a single activity.

An activity may have the following status names:

* *Idle* if the activity has not been started yet.
* *Waiting* if the activity has been started and is waiting to be executed.
* *Running*  if the activity is currently being executed.
* *Canceling* if the activity has been requested to stop but has not stopped yet.
* *Finished* if the activity has finished execution, either successfully or failed.

Once an activity has been started using the API, the activity transitions to the *Waiting* status and the *isRunning* field switches to *true*.
It will remain in *Waiting* until the execution starts when it transitions to the *Running* status.
If a user cancels the activity during execution, it will transition to *Canceling* and remain there until it actually stops execution.
When the execution finished it transitions to the *Finished* status and *isRunning* switches to *false*.
If the activity execution failed, *failed* will be set to true once the *Finished* status has been reached.

While running, the progress is tracked by the *progress* field (0 to 100 percent).
    """

  final val activityStatusExample =
    """
      {
        "project": "project name",
        "task": "transformation1",
        "activity": "transform",
        "statusName": "...",
        "isRunning": true,
        "progress": 85.2,
        "message": "...",
        "failed": false,
        "lastUpdateTime": 1503998693958,
        "startTime": 1503998693001
      }
    """

}
