package controllers.projectApi.doc

object ProjectApiDoc {

  final val taskLoadingErrorReportJsonExample =
    """
      [
        {
          "taskId": "transformsourcex",
          "errorSummary": "Loading failed: Transform source X",
          "taskLabel": "Transform source X",
          "taskDescription": "Transforms source X to ...",
          "errorMessage": "Loading of task 'Transform source X' failed because input 'some_missing_input' could not be found.",
          "stacktrace": "... <SUPER_LONG_JVM_STACKTRACE> ..."
        }
      ]
    """

  final val taskLoadingErrorReportMarkdownExample =
    """
# Project task loading error report

In project 'cmem' 2 tasks could not be loaded.

## Task 1: Transform source X

* Task ID: transformsourcex
* Error summary: Loading failed: Transform source X,
* Task label: Transform source X
* Task description: Transforms source X to ...
* Error message: Loading of task 'Transform source X' failed because input 'some_missing_input' could not be found.
* Stacktrace:
```
  SUPER LONG JVM STACKTRACE
 ```
"""

  final val reloadFailedTaskRequestExample =
    """{
  "taskId": "<CURRENTLY_BROKEN_TASK>",
  "parameterValues": {
    "parameters": {
      "paramA": "New value",
      "paramB": {
        "nestedParameter": "New nested value"
      }
    },
    "templates": {
      "paramB": "Template value {{variable}}"
    }
  }
}"""

  final val failedTaskParameterValuesResponseExample =
    """{
  "paramA": "New value",
  "paramB": {
    "nestedParameter": "New nested value"
  }
}"""

}
