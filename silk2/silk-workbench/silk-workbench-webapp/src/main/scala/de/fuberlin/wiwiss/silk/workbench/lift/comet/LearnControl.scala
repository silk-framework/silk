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

package de.fuberlin.wiwiss.silk.workbench.lift.comet

import de.fuberlin.wiwiss.silk.util.task.{TaskFinished, TaskStarted, TaskStatus}
import de.fuberlin.wiwiss.silk.workbench.lift.util.{JS, DynamicButton}
import de.fuberlin.wiwiss.silk.learning.active.ActiveLearningTask
import de.fuberlin.wiwiss.silk.workbench.workspace.{TaskDataListener, User, CurrentTaskStatusListener}
import de.fuberlin.wiwiss.silk.learning.individual.Population
import de.fuberlin.wiwiss.silk.workbench.learning._
import java.util.logging.Level

class LearnControl extends DynamicButton {

  override protected val dontCacheRendering = true

  override def render = {
    label = if(CurrentPopulation().isEmpty) "Start" else "Done"
    super.render
  }

  /**
   * Called when the button has been pressed.
   */
  override protected def onPressed() = {

//    val task =
//      new ActiveLearningEvaluator(
//        config = CurrentConfiguration(),
//        sources = User().project.sourceModule.tasks.map(_.source),
//        linkSpec = User().linkingTask.linkSpec,
//        paths = User().linkingTask.cache.entityDescs.map(_.paths),
//        validationEntities = User().linkingTask.cache.entities
//      )
//    task.progressLogLevel = Level.FINEST
//    task.runInBackground()
//    JS.Empty

    if(User().linkingTask.cache.status.isRunning) {
      JS.Message("Cache not loaded yet.")
    }
    else if(label == "Done") {
      CurrentActiveLearningTask().cancel()
      JS.Redirect("/population.html")
    }
    else {
      if (!CurrentActiveLearningTask().status.isRunning) {
        val sampleLinksTask =
          new ActiveLearningTask(
            config = CurrentConfiguration(),
            sources = User().project.sourceModule.tasks.map(_.source),
            linkSpec = User().linkingTask.linkSpec,
            paths = User().linkingTask.cache.entityDescs.map(_.paths),
            referenceEntities = User().linkingTask.cache.entities,
            pool = CurrentPool(),
            population = CurrentPopulation()
          )

        CurrentActiveLearningTask() = sampleLinksTask
        sampleLinksTask.runInBackground()
      } else {
        CurrentActiveLearningTask().cancel()
      }

      label = "Done"

      JS.Empty
    }
  }

  /**
   * Listens to changes of the current active learning task.
   */
  private val activeLearningTaskListener = new CurrentTaskStatusListener(CurrentActiveLearningTask) {
    override def onUpdate(status: TaskStatus) {
      status match {
        case _: TaskFinished => partialUpdate {
          CurrentPool() = task.pool
          CurrentPopulation() = task.population
          CurrentValidationLinks() = task.links
        }
        case _ =>
      }
    }
  }

  /**
   * Listens to changes of the current population.
   */
  private val populationListener = new TaskDataListener(CurrentPopulation) {
    override def onUpdate(population: Population) {
      if(population.isEmpty) {
        label = "Start"
      }
    }
  }
}