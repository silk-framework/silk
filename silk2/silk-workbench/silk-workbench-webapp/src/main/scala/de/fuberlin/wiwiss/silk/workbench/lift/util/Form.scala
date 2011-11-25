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

package de.fuberlin.wiwiss.silk.workbench.lift.util

import java.util.UUID

trait Form {
  /** The id of this form */
  private lazy val id : String = UUID.randomUUID.toString

  /** The fields of this form. */
  val fields : List[Field[_]]

  def updateCmd = fields.map(_.updateValueCmd).reduceLeft(_ & _)

  def render = {
    <div id={id} > {
      <table> {
        for(field <- fields) yield {
          <tr>
            <td>
            { field.label }
            </td>
            <td>
            { field.render }
            </td>
          </tr>
        }
      }
      </table>
    }
    </div>
  }
}