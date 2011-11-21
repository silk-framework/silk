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

package de.fuberlin.wiwiss.silk.workbench.lift.snippet

import de.fuberlin.wiwiss.silk.workbench.workspace.modules.source.SourceTask
import de.fuberlin.wiwiss.silk.workbench.workspace.User
import de.fuberlin.wiwiss.silk.util.Identifier
import de.fuberlin.wiwiss.silk.workbench.workspace.modules.linking.LinkingTask
import de.fuberlin.wiwiss.silk.config.DatasetSpecification
import de.fuberlin.wiwiss.silk.workbench.lift.util.{PluginDialog, StringField}
import xml.NodeSeq
import de.fuberlin.wiwiss.silk.datasource.{Source, DataSource}

/**
 * A dialog to create and edit a source task.
 */
object SourceTaskDialog extends PluginDialog[DataSource] {
  override def title = "Source"

  private val nameField = StringField("name", "The name of this source task", () => if (User().sourceTaskOpen) User().sourceTask.name.toString else "")

  override protected val fields = nameField :: Nil

  override protected val plugins = DataSource.availablePlugins.toSeq

  //Close the current task if the window is closed
  override protected def dialogParams = ("close" -> "closeTask") :: super.dialogParams

  override protected def currentObj = {
    if (User().sourceTaskOpen)
      Some(User().sourceTask.source.dataSource)
    else
      None
  }

  override protected def onSubmit(dataSource: DataSource) {
    val newSource = SourceTask(Source(nameField.value, dataSource))

    User().project.sourceModule.update(newSource)

    if (User().sourceTaskOpen && User().sourceTask.name != newSource.name) {
      User().project.sourceModule.remove(User().sourceTask.name)

      //Update all linking tasks to point to the updated task
      val linkingModule = User().project.linkingModule
      val updateFunc = new UpdateLinkingTask(User().sourceTask.name, newSource.name)
      val updatedLinkingTasks = linkingModule.tasks.collect(updateFunc)
      for (linkingTask <- updatedLinkingTasks) {
        linkingModule.update(linkingTask)
      }
    }
  }

  override def render(in: NodeSeq): NodeSeq = super.render(in)

  /**
   * Partial function which updates the source of a linking task.
   */
  private class UpdateLinkingTask(oldSource: Identifier, newSource: Identifier) extends PartialFunction[LinkingTask, LinkingTask] {
    override def isDefinedAt(task: LinkingTask) = {
      task.linkSpec.datasets.exists(_.sourceId == oldSource)
    }

    override def apply(task: LinkingTask) = {
      val updatedLinkSpec = task.linkSpec.copy(datasets = task.linkSpec.datasets.map(updateDataset))
      task.updateLinkSpec(updatedLinkSpec, User().project)
    }

    private def updateDataset(ds: DatasetSpecification) = {
      if (ds.sourceId == oldSource)
        ds.copy(sourceId = newSource)
      else
        ds
    }
  }
}