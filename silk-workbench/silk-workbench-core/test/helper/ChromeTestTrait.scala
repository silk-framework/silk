package helper

import java.util.concurrent.TimeUnit

import org.openqa.selenium.interactions.Actions
import org.openqa.selenium.{TimeoutException, WebElement}
import org.scalatest.selenium.WebBrowser.{Element, Query}
import org.scalatest.{MustMatchers, Suite}
import org.scalatestplus.play.{ChromeFactory, OneBrowserPerSuite}

import scala.language.implicitConversions

/**
  * Created on 7/20/16.
  */
trait ChromeTestTrait extends IntegrationTestTrait with OneBrowserPerSuite with ChromeFactory with MustMatchers { this: Suite =>

  final val DEFAULT_PAGE_LOAD_TIMEOUT: Long = 3000

  implicit def fromElement2WebElement(elem: Element): WebElement = elem.underlying

  def dragAndDrop(from: Query, to: Query): Unit = {
    dragAndDrop(from.element, to.element)
  }

  def dragAndDrop(from: Element, to: Element): Unit = {
    val builder = new Actions(webDriver)
    val action = builder.clickAndHold(from)
        .moveToElement(to)
        .release(to)
        .build()
    action.perform()
  }

  /**
    * Sets the timeout as defied by the timeoutMs parameter.
    * Returns the Some(result) if no timeout has occurred,
    * else if a timeout has occurred returns None.
    */
  def withIgnoredPageLoadTimeout[T](block: => T, timeoutMs: Long = DEFAULT_PAGE_LOAD_TIMEOUT): Option[T] = {
    implicit val timeouts = webDriver.manage().timeouts().pageLoadTimeout(timeoutMs, TimeUnit.MILLISECONDS)
    try {
      Some(block)
    } catch {
      case e: TimeoutException =>
      None
    } finally {
      webDriver.manage().timeouts().pageLoadTimeout(-1, TimeUnit.MILLISECONDS)
    }
  }

  def elementExists(elementId: String): Unit = {
    id(elementId).findElement mustBe defined
  }
}
