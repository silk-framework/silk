package de.fuberlin.wiwiss.silk.workbench.lift.snippet

import de.fuberlin.wiwiss.silk.workbench.lift.util.Dialog

/**
 * Base class of CreateSourceTaskDialog and EditSourceTaskDialog.
 */
trait SourceTaskDialog extends Dialog
{
  val nameField = StringField("Name", "The name of this source task", () => "")

  val uriField = StringField("Endpoint URI", "The URI of the SPARQL endpoint", () => getParam("endpointURI"))

  val graphField = StringField("Graph URI", "Only retrieve instances from a specific graph", () => getParam("graph"))

  val retryCountField = StringField("Retry count", "To recover from intermittent SPARQL endpoint connection failures, " +
                   "the 'retryCount' parameter specifies the number of times to retry connecting.", () => getParam("retryCount"))

  val retryPauseField = StringField("Retry pause", "To recover from intermittent SPARQL endpoint connection failures, " +
                   "the 'retryPause' parameter specifies how long to wait between retries.", () => getParam("retryPause"))

  //Close the current task if the window is closed
  override protected def dialogParams = ("close" -> "closeTask") :: super.dialogParams

  /**
   * Gets a parameter.
   */
  protected def getParam(name : String) : String
}