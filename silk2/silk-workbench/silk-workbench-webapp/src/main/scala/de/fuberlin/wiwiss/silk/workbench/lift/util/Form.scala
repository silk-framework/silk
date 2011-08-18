package de.fuberlin.wiwiss.silk.workbench.lift.util

import java.util.UUID

trait Form
{
  /** The id of this form */
  private lazy val id : String = UUID.randomUUID.toString

  /** The fields of this form. */
  val fields : List[Field[_]]

  def updateCmd = fields.map(_.updateValueCmd).reduceLeft(_ & _)

  def render =
  {
    <div id={id} >
    {
        <table>
        {
          for(field <- fields) yield
          {
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