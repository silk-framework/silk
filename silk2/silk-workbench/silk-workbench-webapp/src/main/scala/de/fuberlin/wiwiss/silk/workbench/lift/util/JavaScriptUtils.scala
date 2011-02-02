package de.fuberlin.wiwiss.silk.workbench.lift.util

import net.liftweb.http.js.JE.{Call, JsRaw, JsVar}
import net.liftweb.http.SHtml
import net.liftweb.http.js.JsCmds.{After, Function}
import net.liftweb.util.Helpers.TimeSpan
import net.liftweb.http.js.{JsCmds, JsExp, JsCmd}
import net.liftweb.http.js.JsCmds.{Script, JsReturn}
import net.liftweb.util.Helpers._
import xml.Node

object JavaScriptUtils
{
  //Injects a Javascript function into HTML
  def injectFunction(name : String, func : String => Unit) : Node =
  {
    //Callback which executes the provided function
    def callback(args : String) : JsCmd =
    {
      val Array(input, successFunc) = args.split(',')

      try
      {
        func(input)

        JsRaw("successFunc('" + input +  "')").cmd
      }
      catch
      {
        case ex : Exception => JsRaw("alert('" + ex.getMessage.encJs + "');").cmd
      }
    }

    //Ajax Call which executes the callback
    val ajaxCall = SHtml.ajaxCall(JsRaw("input + ',' + successFunc"), callback _)._2.cmd

    //JavaScript function definition
    val functionDef = JsCmds.Function(name, "input" :: "successFunc" :: Nil, ajaxCall)

    Script(functionDef)
  }

  def PeriodicUpdate(updateFunc : () => JsCmd, interval : Int = 1000) =
  {
    Function("update", Nil, SHtml.ajaxInvoke(updateFunc)._2.cmd & After(TimeSpan(interval), Call("update").cmd)) & Call("update").cmd
  }
}
