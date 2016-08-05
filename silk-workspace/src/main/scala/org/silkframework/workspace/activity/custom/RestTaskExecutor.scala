package org.silkframework.workspace.activity.custom

import org.silkframework.plugins.custom.net.RestTaskSpec
import org.silkframework.runtime.activity.{Activity, ActivityContext}
import org.silkframework.workspace.ProjectTask
import play.api.libs.ws.ning.{NingAsyncHttpClientConfigBuilder, NingWSClient}
import play.api.libs.ws.{DefaultWSClientConfig, WSClient, WSResponse}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}


/**
  * Created on 8/2/16.
  */
case class RestTaskExecutor(task: ProjectTask[RestTaskSpec]) extends Activity[Unit] {
  /**
    * Executes this activity.
    *
    * @param context Holds the context in which the activity is executed.
    */
  override def run(context: ActivityContext[Unit]): Unit = {
    var client: NingWSClient = null
    try {
      val restTaskSpec = {
        task.data match {
          case s: RestTaskSpec => s
          case _ => throw new UnsupportedOperationException("This is not a REST task")
        }
      }
      client = getClient()
      val response: Future[WSResponse] = executeRequest(client, restTaskSpec)
      context.status.update("Execute", 0.5)
      Await.result(response, restTaskSpec.requestTimeout.millis)
      context.status.update("Finished", 1.0)
    } finally {
      if(client != null) {
        client.close()
      }
    }
  }


  private def executeRequest(client: WSClient,
                             restTaskSpec: RestTaskSpec): Future[WSResponse] = {
    val wsClient = getClient()
    import restTaskSpec._
    var request = wsClient.url(url)
    if (accept.trim != "") {
      request = request.withHeaders("Accept" -> accept)
    }
    if (!Set("GET", "PUT", "POST").contains(method.toUpperCase)) {
      throw new IllegalArgumentException("Value for parameter 'method' must be either GET, POST or PUT.")
    } else {
      request = request.withMethod(method.toUpperCase)
    }
    request = request.withRequestTimeout(requestTimeout)
    // TODO: Add content if defined
    request.execute()
  }

  private def getClient(): NingWSClient = {
    val clientConfig = new DefaultWSClientConfig()
    val secureDefaults: com.ning.http.client.AsyncHttpClientConfig = new NingAsyncHttpClientConfigBuilder(clientConfig).build()
    val builder = new com.ning.http.client.AsyncHttpClientConfig.Builder(secureDefaults)
    builder.setCompressionEnabled(true)
    val secureDefaultsWithSpecificOptions: com.ning.http.client.AsyncHttpClientConfig = builder.build()
    new play.api.libs.ws.ning.NingWSClient(secureDefaultsWithSpecificOptions)
  }
}
