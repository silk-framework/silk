package org.silkframework.runtime.templating.exceptions

import org.silkframework.config.{Task, TaskSpec}
import org.silkframework.runtime.validation.RequestException
import org.silkframework.workbench.utils.JsonRequestException
import play.api.libs.json.{JsObject, Json}

import java.net.HttpURLConnection

/**
  * Thrown if the user modify variables in a way that breaks tasks that use those variables.
  * For instance, if a variable is deleted that is being used.
  */
class CannotModifyVariablesUsedByTaskException(message: String, taskId: String, cause: Throwable)
  extends RequestException(message, Some(cause)) with JsonRequestException {

  /**
    * A short error title.
    */
  override val errorTitle: String = "Cannot modify variables"

  /**
    * The HTTP error code.
    */
  override val httpErrorCode: Option[Int] = Some(HttpURLConnection.HTTP_BAD_REQUEST)

  /**
    * Json that will be included in addition to the HTTP Problem details JSON.
    */
  override def additionalJson: JsObject = {
    Json.obj(
      "taskId" -> taskId
    )
  }
}

case class CannotDeleteVariableUsedByTaskException(variableName: String, task: Task[_ <: TaskSpec], cause: Throwable)
  extends CannotModifyVariablesUsedByTaskException(s"Cannot delete variable '$variableName', because it is used in task ${task.labelAndId}", task.id, cause) {

  override val errorTitle: String = "Cannot delete variable"
}

case class CannotUpdateVariableUsedByTaskException(variableName: String, task: Task[_ <: TaskSpec], cause: Throwable)
  extends CannotModifyVariablesUsedByTaskException(s"Cannot update variable '$variableName', because of its usage in task ${task.labelAndId}: ${cause.getMessage}",
                                                   task.id, cause) {

  override val errorTitle: String = "Cannot update variable"
}

case class CannotUpdateVariablesUsedByTaskException(task: Task[_ <: TaskSpec], cause: Throwable)
  extends CannotModifyVariablesUsedByTaskException(s"Cannot update variables, because of their usage in task ${task.labelAndId}: ${cause.getMessage}",
    task.id, cause) {

  override val errorTitle: String = "Cannot update variables"
}