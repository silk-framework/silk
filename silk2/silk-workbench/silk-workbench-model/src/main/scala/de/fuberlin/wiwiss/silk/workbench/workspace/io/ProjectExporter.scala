/* 
 * Copyright 2011 Freie Universität Berlin, MediaEvent Services GmbH & Co. KG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.fuberlin.wiwiss.silk.workbench.workspace.io

import de.fuberlin.wiwiss.silk.config.Prefixes
import de.fuberlin.wiwiss.silk.workbench.workspace.Project
import de.fuberlin.wiwiss.silk.workbench.workspace.modules.source.SourceTask
import de.fuberlin.wiwiss.silk.workbench.workspace.modules.linking.LinkingTask

/**
 * Exports a project to a single XML file.
 */
object ProjectExporter
{
  def apply(project : Project) =
  {
    implicit val prefixes = project.config.prefixes

    <Project>
      <Config>
      { prefixes.toXML }
      </Config>
      <SourceModule>
        <Tasks>
        {
          for(task <- project.sourceModule.tasks) yield exportSourceTask(task)
        }
        </Tasks>
      </SourceModule>
      <LinkingModule>
        <Tasks>
        {
          for(task <- project.linkingModule.tasks) yield exportLinkingTask(task)
        }
        </Tasks>
      </LinkingModule>
    </Project>
  }

  private def exportSourceTask(task : SourceTask) =
  {
    <SourceTask>
    {
      task.source.toXML
    }
    </SourceTask>
  }

  private

  def exportLinkingTask(task : LinkingTask)(implicit prefixes : Prefixes) =
  {
    <LinkingTask>
      <LinkSpecification>{task.linkSpec.toXML}</LinkSpecification>
      <Alignment>{task.referenceLinks.toXML}</Alignment>
      <Cache>{task.cache.toXML}</Cache>
    </LinkingTask>
  }
}