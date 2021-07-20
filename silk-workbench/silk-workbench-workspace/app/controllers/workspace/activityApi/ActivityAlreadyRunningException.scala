package controllers.workspace.activityApi

import org.silkframework.runtime.validation.RequestException

import java.net.HttpURLConnection

/**
  * Thrown if the user tries to start a singleton activity that is already running.
  */
case class ActivityAlreadyRunningException(activityName: String) extends RequestException(s"Cannot start activity '$activityName'. Already running.", cause = None) {

  /**
    * A short error title, e.g, "Task not found".
    */
  override val errorTitle: String = "Activity already running"

  /**
    * The HTTP error code. Typically in the 4xx range.
    */
  override val httpErrorCode: Option[Int] = Some(HttpURLConnection.HTTP_BAD_REQUEST)

}
