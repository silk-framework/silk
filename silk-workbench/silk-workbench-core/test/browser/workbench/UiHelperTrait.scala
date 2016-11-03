package browser.workbench

import org.scalatestplus.play.OneBrowserPerSuite

/**
  * A Trait that can be mixed into Browser based tests and offers some helper methods specific for the DataIntegration
  * user interface.
  */
trait UiHelperTrait extends { this: OneBrowserPerSuite =>
  def jsPlumbInputPort(operatorId: String): Query = {
    cssSelector(s"#$operatorId ~ ._jsPlumb_endpoint ~ ._jsPlumb_endpoint")
  }

  def jsPlumbOutputPort(operatorId: String): Query = {
    cssSelector(s"#$operatorId ~ ._jsPlumb_endpoint")
  }

  def eventuallyIsDisplayed(query: Query): Unit = {
    eventuallyIsTrue {
      query.element.isDisplayed
    }
  }

  def eventuallyIsTrue(block: => Boolean): Unit = {
    eventually {
      if(!block) {
        throw new RuntimeException("Condition is not yet true.")
      }
    }
  }

  def select(selectElementId: String, value: String): Unit = {
    executeScript(s"$$('#$selectElementId').val('$value')")
  }
}
