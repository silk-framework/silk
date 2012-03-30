/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.fuberlin.wiwiss.silk.workbench.lift.comet

import de.fuberlin.wiwiss.silk.config.LinkSpecification
import org.apache.http.client.HttpClient
import org.apache.http.params.HttpConnectionParams
import org.apache.http.impl.client.DefaultHttpClient
import java.util.ArrayList
import org.apache.http.message.BasicNameValuePair
import org.apache.http.{HttpStatus, HttpResponse, NameValuePair}
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.{HttpPut, HttpPost, HttpGet}
import java.util.logging.{Level, Logger}

class RepositoryWriter(repositoryName: String,
                        repositorySpec: Map[String, String]
                        ) {
  private val logger = Logger.getLogger(classOf[Editor].getName)

  def post(linkSpec: LinkSpecification, linkSpecString: String) {
    val repositoryUrl = repositorySpec.getOrElse("url", "")
    val repositoryApiKey = repositorySpec.getOrElse("apiKey", "")

    // check if link spec exists
    val getTaskUrl = repositoryUrl + "/task/" + linkSpec.id.toString + "/configuration"
    val httpClient:HttpClient = new DefaultHttpClient()
    val httpGet: HttpGet = new HttpGet(getTaskUrl)
    val httpParams = httpClient.getParams
    HttpConnectionParams.setConnectionTimeout(httpParams, 15000)
    HttpConnectionParams.setSoTimeout(httpParams, 15000)
    val httpResponse = httpClient.execute(httpGet)

    val entity = httpResponse.getEntity()
    var content = ""
    if (entity != null) {
      val inputStream = entity.getContent()
      content = io.Source.fromInputStream(inputStream).getLines.mkString
      inputStream.close
    }
    httpClient.getConnectionManager().shutdown()

    if (content.startsWith("<html>")) {
      // post initial link spec
      val httpClient:HttpClient = new DefaultHttpClient()
      val httpPost: HttpPost = new HttpPost(repositoryUrl + "/tasks")

      val params:Map[String, String] = Map("specification" -> linkSpecString, "title" -> linkSpec.id.toString, "api_key" -> repositoryApiKey)
      val formParams = new ArrayList[NameValuePair]()
      params.foreach { e => formParams.add(new BasicNameValuePair(e._1, e._2)) }

      httpPost.setEntity(new UrlEncodedFormEntity(formParams))

      val response: HttpResponse = httpClient.execute(httpPost)

      // Check response code
      if (response != HttpStatus.SC_CREATED) {
        // TODO throw new Exception("Received error status " + response)
      }
      httpClient.getConnectionManager().shutdown()
    } else {
      // overwrite link spec
      val putUrl = repositoryUrl + "/task/"+linkSpec.id.toString+"/configuration"
      val httpClient:HttpClient = new DefaultHttpClient()
      val httpPut: HttpPut = new HttpPut(putUrl)

      val params:Map[String, String] = Map("configuration" -> linkSpecString, "api_key" -> repositoryApiKey)
      val formParams = new ArrayList[NameValuePair]()
      params.foreach { e => formParams.add(new BasicNameValuePair(e._1, e._2)) }
      httpPut.setEntity(new UrlEncodedFormEntity(formParams))

      val response: HttpResponse = httpClient.execute(httpPut)

      // Check response code
      if (response != HttpStatus.SC_CREATED) {
        // TODO throw new Exception("Received error status " + response)
      }
      httpClient.getConnectionManager().shutdown()
    }
    logger.log(Level.INFO, "Writing task "+linkSpec.id.toString+" to repository "+repositoryName)
  }
}