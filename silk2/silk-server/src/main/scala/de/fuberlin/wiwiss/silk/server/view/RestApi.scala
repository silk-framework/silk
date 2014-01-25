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

package de.fuberlin.wiwiss.silk.server.view

import net.liftweb.http._
import net.liftweb.common.{Empty, Full}
import de.fuberlin.wiwiss.silk.plugins.jena.RdfDataSource
import de.fuberlin.wiwiss.silk.server.model.Server
import de.fuberlin.wiwiss.silk.datasource.{Source}
import de.fuberlin.wiwiss.silk.util.plugin.EmptyResourceManager

object RestApi {
  def dispatch : LiftRules.DispatchPF = {
    case req @ Req(List("api", "process"), "", PostRequest) => () => generateLinks(req)
    case Req(List(_), "", _) => () => Empty
  }

  private def generateLinks(req : Req) = {
    val input = new String(getLoad(req), "UTF-8")
    val format = req.param("format").getOrElse("RDF/XML")
    val source = new Source("Input", new RdfDataSource(input, format))

    val response = Server.process(source)

    Full(PlainTextResponse(response, Nil, 200))
  }

  private def getLoad(req : Req) : Array[Byte] ={
    if(!req.uploadedFiles.isEmpty)
      req.uploadedFiles.head.file
    else if(!req.body.isEmpty)
      req.body.open_!
    else
      Array.empty
  }
}
