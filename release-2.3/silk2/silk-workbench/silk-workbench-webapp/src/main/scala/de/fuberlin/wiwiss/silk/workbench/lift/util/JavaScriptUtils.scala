package de.fuberlin.wiwiss.silk.workbench.lift.util

import net.liftweb.http.js.JE.Call
import net.liftweb.http.SHtml
import net.liftweb.http.js.JsCmds.{After, Function}
import net.liftweb.http.js.JsCmd
import net.liftweb.util.Helpers.TimeSpan

object JavaScriptUtils
{
  def PeriodicUpdate(updateFunc : () => JsCmd, interval : Int = 1000) =
  {
    Function("update", Nil, SHtml.ajaxInvoke(updateFunc)._2.cmd & After(TimeSpan(interval), Call("update").cmd)) & Call("update").cmd
  }
}
