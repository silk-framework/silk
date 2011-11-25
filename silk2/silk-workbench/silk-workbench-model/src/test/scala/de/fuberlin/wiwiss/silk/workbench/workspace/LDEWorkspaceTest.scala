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

package de.fuberlin.wiwiss.silk.workbench.workspace

import modules.linking.LinkingTask
import modules.source.SourceTask
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import de.fuberlin.wiwiss.silk.plugins.Plugins
import de.fuberlin.wiwiss.silk.config.{LinkFilter, LinkSpecification}
import java.net.URI
import de.fuberlin.wiwiss.silk.datasource.{DataSource, Source}

class LDEWorkspaceTest extends FlatSpec with ShouldMatchers
{
    Plugins.register()

    // Test LDE backend on a local TripleStore

    val ws = new LDEWorkspace(new URI("http://localhost:8092"))
    val projectName = "testProject"

    it should "create/remove project" in
    {
    ws.createProject(projectName)
        ws.project(projectName).name should equal (projectName)
        ws.removeProject(projectName)
        evaluating{ws.project(projectName)} should produce [NoSuchElementException]
    }

    it should "create/remove datasoure" in
    {
        ws.createProject(projectName)

        val sources = ws.project(projectName).sourceModule

        // test new project contains a default TARGET datasource
        sources.task("TARGET").source.dataSource match {case DataSource(_, p) => {p("label").toString should equal ("Wiki")}}


        // test insert SOURCE datasource - user can only insert a datasource which exists in the TS - using/assuming ABA or KEGGGene
        val newSource = SourceTask(Source("ABA", DataSource("LDEsparqlEndpoint",  Map("endpointURI" -> ""))))
        sources.update(newSource)
          // name 'SOURCE' is forced
        evaluating{sources.task("ABA")} should produce [NoSuchElementException]
        sources.task("SOURCE").source.dataSource match {case DataSource(_, p) => {p("label").toString should equal ("ABA")}}

        // test change SOURCE datasource
        // TODO -

        // test remove TARGET datasource - not allowed
        sources.remove("TARGET")
        sources.task("TARGET").source.dataSource match {case DataSource(_, p) => {p("label").toString should equal ("Wiki")}}

        // test remove SOURCE datasource
        sources.remove("SOURCE")
        evaluating{sources.task("SOURCE")} should produce [NoSuchElementException]


        ws.removeProject(projectName)
        evaluating{ws.project(projectName)} should produce [NoSuchElementException]
    }


}

