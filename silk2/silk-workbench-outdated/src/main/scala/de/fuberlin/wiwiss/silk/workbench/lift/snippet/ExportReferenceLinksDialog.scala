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

package de.fuberlin.wiwiss.silk.workbench.lift.snippet

import de.fuberlin.wiwiss.silk.workspace.User
import de.fuberlin.wiwiss.silk.workbench.lift.util.{StringField, SelectField, JS, Dialog}

object ExportReferenceLinksDialog extends Dialog {

  override val title = "Generate Links"
  
  private val noOutputName = "No Output"
 
  private def outputs = noOutputName :: User().project.outputModule.tasks.toList.map(_.name.toString)
                    
  private val posOutputField = SelectField("Positive Links", "The output where the positive reference links are written", () => outputs, () => outputs.tail.headOption.getOrElse(noOutputName))

  private val posPredicateField = StringField("Property", "The property used for the positive reference links", () => User().linkingTask.linkSpec.linkType.toString)
  
  private val negOutputField = SelectField("Negative Links", "The output where the negative reference links are written", () => outputs, () => noOutputName)

  private val negPredicateField = StringField("Property", "The property used for the negative reference links", () => "http://www.w3.org/2002/07/owl#differentFrom")

  override val fields = posOutputField :: posPredicateField :: negOutputField :: negPredicateField :: Nil

  override def openCmd = {
    if(User().project.outputModule.tasks.isEmpty)
      JS.Message("The currrent project does not define any output. Please define an output to which the reference links can be written to.")
    else
      super.openCmd
  }
  
  override protected def onSubmit() = {
    val outputModule = User().project.outputModule
    val referenceLinks = User().linkingTask.referenceLinks
    
    if(posOutputField.value != noOutputName) {
      val posOutput = outputModule.task(posOutputField.value).output
      posOutput.writeAll(referenceLinks.positive, posPredicateField.value)
    }

    if(negOutputField.value != noOutputName) {
      val negOutput = outputModule.task(negOutputField.value).output
      negOutput.writeAll(referenceLinks.negative, negPredicateField.value)
    }

    JS.Empty
  }
}
