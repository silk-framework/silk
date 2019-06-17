package org.silkframework.workbench.filters

import akka.stream.Materializer
import javax.inject.Inject
import play.api.http.HeaderNames._
import play.api.mvc.{Filter, RequestHeader, Result}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

/**
  * Add HTTP caching headers to responses
  */
class HttpCaching @Inject()(implicit val mat: Materializer, ec: ExecutionContext) extends Filter {

  def apply (nextFilter: (RequestHeader) => Future[Result])(requestHeader: RequestHeader): Future[Result] = {
    nextFilter(requestHeader) map { result =>
      result.header.headers.get(CACHE_CONTROL) match {
        case None =>
          // Only set caching header if not already set
          result.withHeaders(
            CACHE_CONTROL -> "no-cache, no-store, max-age=0, must-revalidate"
          )
        case _ =>
          result
      }
    }
  }
}