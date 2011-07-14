package de.fuberlin.wiwiss.silk.workbench.lift.util

import xml.NodeSeq
import net.liftweb.http.js.JsCmds._
import de.fuberlin.wiwiss.silk.util.strategy.{Strategy, Parameter, StrategyDefinition}

/**
 * A form which allows the user to create instances of a specific strategy.
 */
class StrategyForm[T <: Strategy](val strategy : StrategyDefinition[T], currentObj : () => Option[T])
{
  private val fields = strategy.parameters.map(createField)

  /**
   * Renders this form to HTML.
   */
  def render() : NodeSeq =
  {
    <div id={"strategy-" + strategy.id}>
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

  /**
   * Updates this form.
   */
  def updateCmd(selectedStrategy : StrategyDefinition[T]) =
  {
    val cmd = fields.map(_.updateValueCmd).fold(JS.Empty)(_ & _)

    if(strategy.id == selectedStrategy.id)
    {
      cmd & JsShowId("strategy-" + strategy.id)
    }
    else
    {
      cmd & JsHideId("strategy-" + strategy.id)
    }
  }

  /**
   * Creates a new instance of the strategy based on the entered values.
   */
  def create() =
  {
    val paramValues = fields.map(field => (field.label, field.value)).toMap

    strategy(paramValues)
  }

  private def createField(param : Parameter) =
  {
    def value() =
    {
      currentObj() match
      {
        case Some(obj) if obj.strategyId == strategy.id => param(obj).toString
        case _ => param.defaultValue.getOrElse("").toString
      }
    }

    StringField(param.name, param.description, value)
  }


}