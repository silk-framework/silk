package browser.workbench

import org.scalatestplus.play.OneBrowserPerSuite

/**
  * A Trait that can be mixed into Browser based tests and offers some helper methods specific for the DataIntegration
  * user interface.
  */
trait UiHelperTrait extends { this: OneBrowserPerSuite =>
  /**
    * Returns the JsPlumb input port element. The index chooses which input port should be returned.
    * @param operatorId id of the workflow operator
    * @param portIndex The index of the input port. Make sure that the operator has at least that number of input ports, else
    *              a port of another operator might be chosen.
    */
  def jsPlumbInputPort(operatorId: String, portIndex: Int = 0): Query = {
    val steps = " ~ .jsplumb-endpoint" * portIndex
    cssSelector(s"#$operatorId ~ .jsplumb-endpoint ~ .jsplumb-endpoint$steps")
  }

  def jsPlumbOutputPort(operatorId: String): Query = {
    cssSelector(s"#$operatorId ~ .jsplumb-endpoint")
  }

  def eventuallyIsDisplayed(query: Query): Unit = {
    eventuallyIsTrue {
      query.element.isDisplayed
    }(errorMessage = "Element corresponding to query " + query.toString + "is never displayed.")
  }

  def eventuallyIsTrue(block: => Boolean)(errorMessage: String = ""): Unit = {
    eventually {
      if(!block) {
        throw new RuntimeException("Condition is not yet true. " + errorMessage)
      }
    }
  }

  @deprecated("use singleSel() or multiSel() from org.scalatest.selenium.WebBrowser instead", since = "v3.3.2")
  def select(selectElementId: String, value: String): Unit = {
    executeScript(s"$$('#$selectElementId').val('$value')")
  }
}
