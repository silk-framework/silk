package de.fuberlin.wiwiss.silk.workbench.lift.snippet

import xml.NodeSeq
import net.liftweb.util.BindHelpers._
import net.liftweb.http.SHtml
import de.fuberlin.wiwiss.silk.workbench.workspace.User
import de.fuberlin.wiwiss.silk.workbench.learning.{CurrentSampleLinksTask, CurrentLearningTask, SampleLinksTask}

class SampleLinksToolbar {
  def render(xhtml: NodeSeq): NodeSeq = {
    bind("entry", xhtml,
         "start" -> SHtml.button("Start", start))
  }

  def start() {
      val sampleLinksTask =
        new SampleLinksTask(
          sources = User().project.sourceModule.tasks.map(_.source),
          linkSpec = User().linkingTask.linkSpec,
          paths = User().linkingTask.cache.instanceSpecs.map(_.paths),
          referenceInstances = User().linkingTask.cache.instances,
          population = CurrentLearningTask().value.get.population
        )

    CurrentSampleLinksTask() = sampleLinksTask
    sampleLinksTask.runInBackground()
  }
}