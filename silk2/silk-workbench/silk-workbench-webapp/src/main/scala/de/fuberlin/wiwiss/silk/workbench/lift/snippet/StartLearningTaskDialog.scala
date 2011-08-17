package de.fuberlin.wiwiss.silk.workbench.lift.snippet

import net.liftweb.http.LiftScreen
import java.util.UUID
import net.liftweb.http.js.JsCmds.{OnLoad, Script}
import net.liftweb.http.js.JE.JsRaw
import xml.NodeSeq
import de.fuberlin.wiwiss.silk.workbench.lift.util.{BooleanField, Dialog}

object StartLearningTaskDialog extends Dialog {

  override val title = "Start learning task"

  private val learnThreshold = BooleanField("Learn Threshold", "Learn Threshold")

  override val fields = learnThreshold :: Nil

  override def onSubmit() {

  }
}