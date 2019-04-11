package org.silkframework.workbench.utils

import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, MustMatchers}
import play.api.http.MediaRange
import play.api.mvc.RequestHeader

/**
  * Silk error test handler tests
  */
class SilkErrorHandlerTest extends FlatSpec with MustMatchers with MockitoSugar {
  behavior of "Silk Error Handler"

  it should "return the right content type if JSON and HTML are requested and JSON is coming first" in {
    val request = mock[RequestHeader]
    when(request.acceptedTypes).thenReturn(mediaRanges("application/json", "text/html"))
    SilkErrorHandler.prefersHtml(request) mustBe false
  }

  it should "return the right content type if JSON is requested without HTML" in {
    val request = mock[RequestHeader]
    when(request.acceptedTypes).thenReturn(mediaRanges("application/json"))
    SilkErrorHandler.prefersHtml(request) mustBe false
  }

  it should "return the right content type if JSON and HTML are requested and HTML is coming first" in {
    val request = mock[RequestHeader]
    when(request.acceptedTypes).thenReturn(mediaRanges("text/html", "application/json"))
    SilkErrorHandler.prefersHtml(request) mustBe true
  }

  it should "return the right content type if HTML is requested without JSON" in {
    val request = mock[RequestHeader]
    when(request.acceptedTypes).thenReturn(mediaRanges("text/html"))
    SilkErrorHandler.prefersHtml(request) mustBe true
  }

  it should "return the right content type if neither HTML nor JSON" in {
    val request = mock[RequestHeader]
    when(request.acceptedTypes).thenReturn(mediaRanges())
    SilkErrorHandler.prefersHtml(request) mustBe false
  }

  private def mediaRanges(mimeTypes: String*): Seq[MediaRange] = {
    val mediaRanges = for(mimeType <- mimeTypes) yield {
      val Array(mediaType, mediaSubType) = mimeType.split("/")
      new MediaRange(mediaType, mediaSubType, Seq(), None, Seq())
    }
    mediaRanges.toList
  }
}
