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
  def PeriodicUpdate(updateFunc : () => JsCmd, interval : Int = 1000) =
  {
    Function("update", Nil, SHtml.ajaxInvoke(updateFunc)._2.cmd & After(TimeSpan(interval), Call("update").cmd)) & Call("update").cmd
  }
}
