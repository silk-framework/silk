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

package de.fuberlin.wiwiss.silk.workbench.lift.bootstrap.liftweb

import de.fuberlin.wiwiss.silk.workbench.workspace.User
import de.fuberlin.wiwiss.silk.workbench.workspace.io.ProjectExporter._
import net.liftweb.common.Full._
import net.liftweb.http.InMemoryResponse._
import xml.{Node, Elem, PrettyPrinter}
import net.liftweb.http._
import net.liftweb.common.{Empty, Full}

object LinkingApi {
  def dispatch : LiftRules.DispatchPF = {
    case req @ Req(List("api", project, "linking", task, "linkSpec"), "", GetRequest) =>
      respond(getTask(project, task).linkSpec.toXML)
    case req @ Req(List("api", project, "linking", task, "referenceLinks"), "", GetRequest) =>
      respond(getTask(project, task).referenceLinks.toXML)
    case req @ Req(List("api", project, "linking", task, "linkSpec"), "", PutRequest) =>
      req.body match {
        case Full(data) => error("Not implemented")
        case Empty => error("Empty Body")
      }
  }

  private def getTask(project: String, task: String) = {
    User().workspace.project(project).linkingModule.task(task)
  }

  //TODO use XmlResponse instead?
  private def respond(xml: Node) = () => {
    val projectStr = new PrettyPrinter(140, 2).format(xml)
    Full(InMemoryResponse(projectStr.getBytes, ("Content-Type", "application/xml") :: ("Content-Disposition", "attachment") :: Nil, Nil, 200))
  }

  //TODO include message
  private def error(msg: String) = () => {
    Full(BadResponse())
  }
}