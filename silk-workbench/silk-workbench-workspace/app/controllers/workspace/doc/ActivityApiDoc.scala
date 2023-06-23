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

  final val startPrioritizedDoc =
"""Starts this activity immediately.
If the activity has already been started, but is not being executed yet, it will skip the waiting queue.
Prioritized activities will not take a slot in the fork join pool. The call returns immediately without waiting for the activity to complete."""

  final val activityErrorReportJsonExample =
  """
{
    "activityId": "ExecuteDefaultWorkflow",
    "errorMessage": "Exception during execution of workflow operator 'Failing transform' (97477cbb-774a-4ad3-b7ef-3096521c7a58_Failingtransform). Cause: Unrecognized token 'name': was expecting (JSON String, Number, Array, Object or token 'null', 'true' or 'false')\n at [Source: (BufferedInputStream); line: 1, column: 6]",
    "errorSummary": "Execution of activity 'ExecuteDefaultWorkflow' has failed.",
    "projectId": "02500aac-492e-45d0-ac3e-68a0da54f56d_Activityintegrationtestproject",
    "projectLabel": "Activity integration test project",
    "stackTrace": {
        "cause": {
            "errorMessage": "Unrecognized token 'name': was expecting (JSON String, Number, Array, Object or token 'null', 'true' or 'false')\n at [Source: (BufferedInputStream); line: 1, column: 6]",
            "lines": [
                "com.fasterxml.jackson.core.JsonParser._constructError(JsonParser.java:2337)",
                "com.fasterxml.jackson.core.base.ParserMinimalBase._reportError(ParserMinimalBase.java:720)",
                "com.fasterxml.jackson.core.json.UTF8StreamJsonParser._reportInvalidToken(UTF8StreamJsonParser.java:3593)",
                "com.fasterxml.jackson.core.json.UTF8StreamJsonParser._reportInvalidToken(UTF8StreamJsonParser.java:3569)",
                "com.fasterxml.jackson.core.json.UTF8StreamJsonParser._matchToken2(UTF8StreamJsonParser.java:2904)",
                "com.fasterxml.jackson.core.json.UTF8StreamJsonParser._matchNull(UTF8StreamJsonParser.java:2875)",
                "com.fasterxml.jackson.core.json.UTF8StreamJsonParser._nextTokenNotInObject(UTF8StreamJsonParser.java:849)",
                "com.fasterxml.jackson.core.json.UTF8StreamJsonParser.nextToken(UTF8StreamJsonParser.java:762)",
                "com.fasterxml.jackson.databind.ObjectMapper._initForReading(ObjectMapper.java:4684)",
                "com.fasterxml.jackson.databind.ObjectMapper._readValue(ObjectMapper.java:4561)",
                "com.fasterxml.jackson.databind.ObjectMapper.readValue(ObjectMapper.java:2798)",
                "play.api.libs.json.jackson.JacksonJson$.parseJsValue(JacksonJson.scala:288)",
                "play.api.libs.json.StaticBinding$.parseJsValue(StaticBinding.scala:21)",
                "play.api.libs.json.Json$.parse(Json.scala:175)",
                "org.silkframework.plugins.dataset.json.JsonSource$.$anonfun$apply$1(JsonSource.scala:227)",
                "org.silkframework.runtime.resource.Resource.read(Resource.scala:59)",
                "org.silkframework.runtime.resource.Resource.read$(Resource.scala:56)",
                "org.silkframework.runtime.resource.FileResource.read(FileResource.scala:13)",
                "org.silkframework.plugins.dataset.json.JsonSource$.apply(JsonSource.scala:227)",
                "org.silkframework.plugins.dataset.json.JsonDataset.source(JsonDataset.scala:35)",
                "org.silkframework.dataset.DatasetSpec.source(DatasetSpec.scala:43)",
                "org.silkframework.execution.local.LocalDatasetExecutor.source$lzycompute$1(LocalDatasetExecutor.scala:29)",
                "org.silkframework.execution.local.LocalDatasetExecutor.source$1(LocalDatasetExecutor.scala:29)",
                "org.silkframework.execution.local.LocalDatasetExecutor.read(LocalDatasetExecutor.scala:40)",
                "org.silkframework.execution.local.LocalDatasetExecutor.read(LocalDatasetExecutor.scala:20)",
                "org.silkframework.execution.DatasetExecutor.$anonfun$execute$2(DatasetExecutor.scala:51)",
                "scala.Option.map(Option.scala:230)",
                "org.silkframework.execution.DatasetExecutor.execute(DatasetExecutor.scala:51)",
                "org.silkframework.execution.DatasetExecutor.execute$(DatasetExecutor.scala:39)",
                "org.silkframework.execution.local.LocalDatasetExecutor.execute(LocalDatasetExecutor.scala:20)",
                "org.silkframework.execution.ExecutorRegistry$.execute(ExecutorRegistry.scala:143)",
                "org.silkframework.workspace.activity.workflow.WorkflowExecutor.execute(WorkflowExecutor.scala:58)",
                "org.silkframework.workspace.activity.workflow.WorkflowExecutor.execute$(WorkflowExecutor.scala:49)",
                "org.silkframework.workspace.activity.workflow.LocalWorkflowExecutor.execute(LocalWorkflowExecutor.scala:34)",
                "org.silkframework.workspace.activity.workflow.LocalWorkflowExecutor.readFromDataset(LocalWorkflowExecutor.scala:271)",
                "org.silkframework.workspace.activity.workflow.LocalWorkflowExecutor.executeWorkflowDataset(LocalWorkflowExecutor.scala:230)",
                "org.silkframework.workspace.activity.workflow.LocalWorkflowExecutor.executeWorkflowNode(LocalWorkflowExecutor.scala:98)",
                "org.silkframework.workspace.activity.workflow.LocalWorkflowExecutor.executeWorkflowOperatorInput(LocalWorkflowExecutor.scala:111)",
                "org.silkframework.workspace.activity.workflow.LocalWorkflowExecutor.$anonfun$executeWorkflowOperatorInputs$2(LocalWorkflowExecutor.scala:175)",
                "scala.collection.immutable.Stream$StreamWithFilter.map(Stream.scala:418)",
                "org.silkframework.workspace.activity.workflow.LocalWorkflowExecutor.executeWorkflowOperatorInputs(LocalWorkflowExecutor.scala:174)",
                "org.silkframework.workspace.activity.workflow.LocalWorkflowExecutor.executeWorkflowOperator(LocalWorkflowExecutor.scala:131)",
                "org.silkframework.workspace.activity.workflow.LocalWorkflowExecutor.executeWorkflowNode(LocalWorkflowExecutor.scala:100)",
                "org.silkframework.workspace.activity.workflow.LocalWorkflowExecutor.$anonfun$runWorkflow$1(LocalWorkflowExecutor.scala:66)",
                "scala.collection.Iterator.foreach(Iterator.scala:943)",
                "scala.collection.Iterator.foreach$(Iterator.scala:943)",
                "scala.collection.AbstractIterator.foreach(Iterator.scala:1431)",
                "scala.collection.IterableLike.foreach(IterableLike.scala:74)",
                "scala.collection.IterableLike.foreach$(IterableLike.scala:73)",
                "scala.collection.AbstractIterable.foreach(Iterable.scala:56)",
                "org.silkframework.workspace.activity.workflow.LocalWorkflowExecutor.runWorkflow(LocalWorkflowExecutor.scala:65)",
                "org.silkframework.workspace.activity.workflow.LocalWorkflowExecutor.run(LocalWorkflowExecutor.scala:50)",
                "org.silkframework.runtime.activity.ActivityExecution.org$silkframework$runtime$activity$ActivityExecution$$runActivity(ActivityExecution.scala:159)",
                "org.silkframework.runtime.activity.ActivityExecution$ForkJoinRunner.exec(ActivityExecution.scala:218)",
                "java.base/java.util.concurrent.ForkJoinTask.doExec(ForkJoinTask.java:290)",
                "java.base/java.util.concurrent.ForkJoinPool$WorkQueue.topLevelExec(ForkJoinPool.java:1020)",
                "java.base/java.util.concurrent.ForkJoinPool.scan(ForkJoinPool.java:1656)",
                "java.base/java.util.concurrent.ForkJoinPool.runWorker(ForkJoinPool.java:1594)",
                "java.base/java.util.concurrent.ForkJoinWorkerThread.run(ForkJoinWorkerThread.java:183)"
            ]
        },
        "errorMessage": "Exception during execution of workflow operator 'Failing transform' (97477cbb-774a-4ad3-b7ef-3096521c7a58_Failingtransform). Cause: Unrecognized token 'name': was expecting (JSON String, Number, Array, Object or token 'null', 'true' or 'false')\n at [Source: (BufferedInputStream); line: 1, column: 6]",
        "lines": [
            "org.silkframework.workspace.activity.workflow.LocalWorkflowExecutor.executeWorkflowOperator(LocalWorkflowExecutor.scala:159)",
            "org.silkframework.workspace.activity.workflow.LocalWorkflowExecutor.executeWorkflowNode(LocalWorkflowExecutor.scala:100)",
            "org.silkframework.workspace.activity.workflow.LocalWorkflowExecutor.$anonfun$runWorkflow$1(LocalWorkflowExecutor.scala:66)",
            "scala.collection.Iterator.foreach(Iterator.scala:943)",
            "scala.collection.Iterator.foreach$(Iterator.scala:943)",
            "scala.collection.AbstractIterator.foreach(Iterator.scala:1431)",
            "scala.collection.IterableLike.foreach(IterableLike.scala:74)",
            "scala.collection.IterableLike.foreach$(IterableLike.scala:73)",
            "scala.collection.AbstractIterable.foreach(Iterable.scala:56)",
            "org.silkframework.workspace.activity.workflow.LocalWorkflowExecutor.runWorkflow(LocalWorkflowExecutor.scala:65)",
            "org.silkframework.workspace.activity.workflow.LocalWorkflowExecutor.run(LocalWorkflowExecutor.scala:50)",
            "org.silkframework.runtime.activity.ActivityExecution.org$silkframework$runtime$activity$ActivityExecution$$runActivity(ActivityExecution.scala:159)",
            "org.silkframework.runtime.activity.ActivityExecution$ForkJoinRunner.exec(ActivityExecution.scala:218)",
            "java.base/java.util.concurrent.ForkJoinTask.doExec(ForkJoinTask.java:290)",
            "java.base/java.util.concurrent.ForkJoinPool$WorkQueue.topLevelExec(ForkJoinPool.java:1020)",
            "java.base/java.util.concurrent.ForkJoinPool.scan(ForkJoinPool.java:1656)",
            "java.base/java.util.concurrent.ForkJoinPool.runWorker(ForkJoinPool.java:1594)",
            "java.base/java.util.concurrent.ForkJoinWorkerThread.run(ForkJoinWorkerThread.java:183)"
        ]
    },
    "taskId": "5c1b7553-5b3d-4533-9d0d-35f2e1fc506e_Failingworkflow",
    "taskLabel": "Failing workflow"
}
  """

  final val activityErrorReportMarkdownExample =
  """
# Activity execution error report

Execution of activity 'ExecuteDefaultWorkflow' in project 'Activity integration test project' of task 'Failing workflow' has failed.

## Details

* Error summary: Execution of activity 'ExecuteDefaultWorkflow' has failed.
* Project ID: 02500aac-492e-45d0-ac3e-68a0da54f56d_Activityintegrationtestproject
* Project label: Activity integration test project
* Task ID: 5c1b7553-5b3d-4533-9d0d-35f2e1fc506e_Failingworkflow
* Task label: Failing workflow
* Activity ID: ExecuteDefaultWorkflow
* Error message: `Exception during execution of workflow operator 'Failing transform' (97477cbb-774a-4ad3-b7ef-3096521c7a58_Failingtransform). Cause: Unrecognized token 'name': was expecting (JSON String, Number, Array, Object or token 'null', 'true' or 'false')
 at [Source: (BufferedInputStream); line: 1, column: 6]`
* Cause error messages:
  - Unrecognized token 'name': was expecting (JSON String, Number, Array, Object or token 'null', 'true' or 'false')
 at [Source: (BufferedInputStream); line: 1, column: 6]
* Stacktrace:
  ```
  org.silkframework.workspace.activity.workflow.LocalWorkflowExecutor.executeWorkflowOperator(LocalWorkflowExecutor.scala:159)
  org.silkframework.workspace.activity.workflow.LocalWorkflowExecutor.executeWorkflowNode(LocalWorkflowExecutor.scala:100)
  org.silkframework.workspace.activity.workflow.LocalWorkflowExecutor.$anonfun$runWorkflow$1(LocalWorkflowExecutor.scala:66)
  scala.collection.Iterator.foreach(Iterator.scala:943)
  scala.collection.Iterator.foreach$(Iterator.scala:943)
  scala.collection.AbstractIterator.foreach(Iterator.scala:1431)
  scala.collection.IterableLike.foreach(IterableLike.scala:74)
  scala.collection.IterableLike.foreach$(IterableLike.scala:73)
  scala.collection.AbstractIterable.foreach(Iterable.scala:56)
  org.silkframework.workspace.activity.workflow.LocalWorkflowExecutor.runWorkflow(LocalWorkflowExecutor.scala:65)
  org.silkframework.workspace.activity.workflow.LocalWorkflowExecutor.run(LocalWorkflowExecutor.scala:50)
  org.silkframework.runtime.activity.ActivityExecution.org$silkframework$runtime$activity$ActivityExecution$$runActivity(ActivityExecution.scala:159)
  org.silkframework.runtime.activity.ActivityExecution$ForkJoinRunner.exec(ActivityExecution.scala:218)
  java.base/java.util.concurrent.ForkJoinTask.doExec(ForkJoinTask.java:290)
  java.base/java.util.concurrent.ForkJoinTask.doJoin(ForkJoinTask.java:396)
  java.base/java.util.concurrent.ForkJoinTask.join(ForkJoinTask.java:721)
  org.silkframework.runtime.activity.ActivityExecution.$anonfun$waitUntilFinished$1(ActivityExecution.scala:134)
  org.silkframework.runtime.activity.ActivityExecution.$anonfun$waitUntilFinished$1$adapted(ActivityExecution.scala:132)
  scala.Option.foreach(Option.scala:407)
  org.silkframework.runtime.activity.ActivityExecution.waitUntilFinished(ActivityExecution.scala:132)
  org.silkframework.workspace.activity.workflow.WorkflowExecutorGeneratingProvenance.run(WorkflowExecutorGeneratingProvenance.scala:34)
  org.silkframework.workspace.activity.workflow.WorkflowExecutorGeneratingProvenance.run$(WorkflowExecutorGeneratingProvenance.scala:22)
  org.silkframework.workspace.activity.workflow.LocalWorkflowExecutorGeneratingProvenance.run(LocalWorkflowExecutorGeneratingProvenance.scala:10)
  org.silkframework.runtime.activity.ActivityExecution.org$silkframework$runtime$activity$ActivityExecution$$runActivity(ActivityExecution.scala:159)
  org.silkframework.runtime.activity.ActivityExecution$ForkJoinRunner.exec(ActivityExecution.scala:218)
  java.base/java.util.concurrent.ForkJoinTask.doExec(ForkJoinTask.java:290)
  java.base/java.util.concurrent.ForkJoinPool$WorkQueue.topLevelExec(ForkJoinPool.java:1020)
  java.base/java.util.concurrent.ForkJoinPool.scan(ForkJoinPool.java:1656)
  java.base/java.util.concurrent.ForkJoinPool.runWorker(ForkJoinPool.java:1594)
  java.base/java.util.concurrent.ForkJoinWorkerThread.run(ForkJoinWorkerThread.java:183)
Cause: Unrecognized token 'name': was expecting (JSON String, Number, Array, Object or token 'null', 'true' or 'false')
 at [Source: (BufferedInputStream); line: 1, column: 6]
  com.fasterxml.jackson.core.JsonParser._constructError(JsonParser.java:2337)
  com.fasterxml.jackson.core.base.ParserMinimalBase._reportError(ParserMinimalBase.java:720)
  com.fasterxml.jackson.core.json.UTF8StreamJsonParser._reportInvalidToken(UTF8StreamJsonParser.java:3593)
  com.fasterxml.jackson.core.json.UTF8StreamJsonParser._reportInvalidToken(UTF8StreamJsonParser.java:3569)
  com.fasterxml.jackson.core.json.UTF8StreamJsonParser._matchToken2(UTF8StreamJsonParser.java:2904)
  com.fasterxml.jackson.core.json.UTF8StreamJsonParser._matchNull(UTF8StreamJsonParser.java:2875)
  com.fasterxml.jackson.core.json.UTF8StreamJsonParser._nextTokenNotInObject(UTF8StreamJsonParser.java:849)
  com.fasterxml.jackson.core.json.UTF8StreamJsonParser.nextToken(UTF8StreamJsonParser.java:762)
  com.fasterxml.jackson.databind.ObjectMapper._initForReading(ObjectMapper.java:4684)
  com.fasterxml.jackson.databind.ObjectMapper._readValue(ObjectMapper.java:4561)
  com.fasterxml.jackson.databind.ObjectMapper.readValue(ObjectMapper.java:2798)
  play.api.libs.json.jackson.JacksonJson$.parseJsValue(JacksonJson.scala:288)
  play.api.libs.json.StaticBinding$.parseJsValue(StaticBinding.scala:21)
  play.api.libs.json.Json$.parse(Json.scala:175)
  org.silkframework.plugins.dataset.json.JsonSource$.$anonfun$apply$1(JsonSource.scala:227)
  org.silkframework.runtime.resource.Resource.read(Resource.scala:59)
  org.silkframework.runtime.resource.Resource.read$(Resource.scala:56)
  org.silkframework.runtime.resource.FileResource.read(FileResource.scala:13)
  org.silkframework.plugins.dataset.json.JsonSource$.apply(JsonSource.scala:227)
  org.silkframework.plugins.dataset.json.JsonDataset.source(JsonDataset.scala:35)
  org.silkframework.dataset.DatasetSpec.source(DatasetSpec.scala:43)
  org.silkframework.execution.local.LocalDatasetExecutor.source$lzycompute$1(LocalDatasetExecutor.scala:29)
  org.silkframework.execution.local.LocalDatasetExecutor.source$1(LocalDatasetExecutor.scala:29)
  org.silkframework.execution.local.LocalDatasetExecutor.read(LocalDatasetExecutor.scala:40)
  org.silkframework.execution.local.LocalDatasetExecutor.read(LocalDatasetExecutor.scala:20)
  org.silkframework.execution.DatasetExecutor.$anonfun$execute$2(DatasetExecutor.scala:51)
  scala.Option.map(Option.scala:230)
  org.silkframework.execution.DatasetExecutor.execute(DatasetExecutor.scala:51)
  org.silkframework.execution.DatasetExecutor.execute$(DatasetExecutor.scala:39)
  org.silkframework.execution.local.LocalDatasetExecutor.execute(LocalDatasetExecutor.scala:20)
  org.silkframework.execution.ExecutorRegistry$.execute(ExecutorRegistry.scala:143)
  org.silkframework.workspace.activity.workflow.WorkflowExecutor.execute(WorkflowExecutor.scala:58)
  org.silkframework.workspace.activity.workflow.WorkflowExecutor.execute$(WorkflowExecutor.scala:49)
  org.silkframework.workspace.activity.workflow.LocalWorkflowExecutor.execute(LocalWorkflowExecutor.scala:34)
  org.silkframework.workspace.activity.workflow.LocalWorkflowExecutor.readFromDataset(LocalWorkflowExecutor.scala:271)
  org.silkframework.workspace.activity.workflow.LocalWorkflowExecutor.executeWorkflowDataset(LocalWorkflowExecutor.scala:230)
  org.silkframework.workspace.activity.workflow.LocalWorkflowExecutor.executeWorkflowNode(LocalWorkflowExecutor.scala:98)
  org.silkframework.workspace.activity.workflow.LocalWorkflowExecutor.executeWorkflowOperatorInput(LocalWorkflowExecutor.scala:111)
  org.silkframework.workspace.activity.workflow.LocalWorkflowExecutor.$anonfun$executeWorkflowOperatorInputs$2(LocalWorkflowExecutor.scala:175)
  scala.collection.immutable.Stream$StreamWithFilter.map(Stream.scala:418)
  org.silkframework.workspace.activity.workflow.LocalWorkflowExecutor.executeWorkflowOperatorInputs(LocalWorkflowExecutor.scala:174)
  org.silkframework.workspace.activity.workflow.LocalWorkflowExecutor.executeWorkflowOperator(LocalWorkflowExecutor.scala:131)
  org.silkframework.workspace.activity.workflow.LocalWorkflowExecutor.executeWorkflowNode(LocalWorkflowExecutor.scala:100)
  org.silkframework.workspace.activity.workflow.LocalWorkflowExecutor.$anonfun$runWorkflow$1(LocalWorkflowExecutor.scala:66)
  scala.collection.Iterator.foreach(Iterator.scala:943)
  scala.collection.Iterator.foreach$(Iterator.scala:943)
  scala.collection.AbstractIterator.foreach(Iterator.scala:1431)
  scala.collection.IterableLike.foreach(IterableLike.scala:74)
  scala.collection.IterableLike.foreach$(IterableLike.scala:73)
  scala.collection.AbstractIterable.foreach(Iterable.scala:56)
  org.silkframework.workspace.activity.workflow.LocalWorkflowExecutor.runWorkflow(LocalWorkflowExecutor.scala:65)
  org.silkframework.workspace.activity.workflow.LocalWorkflowExecutor.run(LocalWorkflowExecutor.scala:50)
  org.silkframework.runtime.activity.ActivityExecution.org$silkframework$runtime$activity$ActivityExecution$$runActivity(ActivityExecution.scala:159)
  org.silkframework.runtime.activity.ActivityExecution$ForkJoinRunner.exec(ActivityExecution.scala:218)
  java.base/java.util.concurrent.ForkJoinTask.doExec(ForkJoinTask.java:290)
  java.base/java.util.concurrent.ForkJoinTask.doJoin(ForkJoinTask.java:396)
  java.base/java.util.concurrent.ForkJoinTask.join(ForkJoinTask.java:721)
  org.silkframework.runtime.activity.ActivityExecution.$anonfun$waitUntilFinished$1(ActivityExecution.scala:134)
  org.silkframework.runtime.activity.ActivityExecution.$anonfun$waitUntilFinished$1$adapted(ActivityExecution.scala:132)
  scala.Option.foreach(Option.scala:407)
  org.silkframework.runtime.activity.ActivityExecution.waitUntilFinished(ActivityExecution.scala:132)
  org.silkframework.workspace.activity.workflow.WorkflowExecutorGeneratingProvenance.run(WorkflowExecutorGeneratingProvenance.scala:34)
  org.silkframework.workspace.activity.workflow.WorkflowExecutorGeneratingProvenance.run$(WorkflowExecutorGeneratingProvenance.scala:22)
  org.silkframework.workspace.activity.workflow.LocalWorkflowExecutorGeneratingProvenance.run(LocalWorkflowExecutorGeneratingProvenance.scala:10)
  org.silkframework.runtime.activity.ActivityExecution.org$silkframework$runtime$activity$ActivityExecution$$runActivity(ActivityExecution.scala:159)
  org.silkframework.runtime.activity.ActivityExecution$ForkJoinRunner.exec(ActivityExecution.scala:218)
  java.base/java.util.concurrent.ForkJoinTask.doExec(ForkJoinTask.java:290)
  java.base/java.util.concurrent.ForkJoinPool$WorkQueue.topLevelExec(ForkJoinPool.java:1020)
  java.base/java.util.concurrent.ForkJoinPool.scan(ForkJoinPool.java:1656)
  java.base/java.util.concurrent.ForkJoinPool.runWorker(ForkJoinPool.java:1594)
  java.base/java.util.concurrent.ForkJoinWorkerThread.run(ForkJoinWorkerThread.java:183)
  ```
  """

  final val activityInstanceParameterDesc = "Optional activity instance identifier. Non-singleton activity types allow multiple concurrent instances that are identified by their instance id."

}
