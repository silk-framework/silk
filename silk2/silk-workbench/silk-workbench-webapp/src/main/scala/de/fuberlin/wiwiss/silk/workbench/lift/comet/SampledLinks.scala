package de.fuberlin.wiwiss.silk.workbench.lift.comet

import de.fuberlin.wiwiss.silk.workbench.evaluation.EvalLink.{Unknown, Incorrect, Generated, Correct}
import de.fuberlin.wiwiss.silk.workbench.evaluation.{CurrentGenerateLinksTask, EvalLink}
import de.fuberlin.wiwiss.silk.workbench.workspace.CurrentTaskStatusListener
import de.fuberlin.wiwiss.silk.util.task.{TaskFinished, TaskRunning, TaskStarted, TaskStatus}
import de.fuberlin.wiwiss.silk.workbench.learning.{SampleLinksTask, CurrentSampleLinksTask}
import net.liftweb.http.js.JsCmds.Alert

class SampledLinks extends LinkList with RateLinkButtons {

  /**Minimum time in milliseconds between two successive updates*/
  private val minUpdatePeriod = 3000L

  /**The time of the last update */
  private var lastUpdateTime = 0L

  override protected val showDetails = false

  override protected val showInstances = true

  override protected val showStatus = false

  private var sampleLinksTask = CurrentSampleLinksTask()

  private val currentSampleLinksTaskListener = (task: SampleLinksTask) => { sampleLinksTask = task }

  CurrentSampleLinksTask.onUpdate(currentSampleLinksTaskListener)

  private val linkListener = new CurrentTaskStatusListener(CurrentSampleLinksTask) {
    override def onUpdate(status: TaskStatus) {
      status match {
        case _: TaskStarted => {}
        case _: TaskRunning if System.currentTimeMillis - lastUpdateTime > minUpdatePeriod => {
          partialUpdate(updateLinksCmd)
          lastUpdateTime = System.currentTimeMillis
        }
        case _: TaskFinished => {
          val cmd = {
            val warnings = CurrentGenerateLinksTask().warnings
            if (warnings.isEmpty) {
              updateLinksCmd
            }
            else {
              updateLinksCmd & Alert("Warnings have been raised during execution:\n- " + warnings.map(_.getMessage).mkString("\n- "))
            }
          }

          partialUpdate(cmd)
        }
        case _ =>
      }
    }
  }

  override protected def links: Seq[EvalLink] = {
    def alignment = linkingTask.alignment

    for (link <- CurrentSampleLinksTask().links.view) yield {
      if (alignment.positive.contains(link)) {
        new EvalLink(link, Correct, Generated)
      } else if (alignment.negative.contains(link)) {
        new EvalLink(link, Incorrect, Generated)
      } else {
        new EvalLink(link, Unknown, Generated)
      }
    }
  }

}