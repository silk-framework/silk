package de.fuberlin.wiwiss.silk.workbench.lift.util

import net.liftweb.http.js.JE.{Call, JsRaw}
import net.liftweb.http.SHtml
import net.liftweb.http.js.JsCmds.{After, Function}
import net.liftweb.http.js.JsCmd
import net.liftweb.util.Helpers._
import java.util.logging.{Level, Logger}

/**
 * Various useful JavaScript commands.
 */
object JS
{
  private val logger = Logger.getLogger(JS.getClass.getName)

  /**
   * Periodically executes a specific JavaScript Command.
   */
  def PeriodicUpdate(updateFunc : () => JsCmd, interval : Int = 1000) =
  {
    Function("update", Nil, SHtml.ajaxInvoke(updateFunc)._2.cmd & After(TimeSpan(interval), Call("update").cmd)) & Call("update").cmd
  }

  /**
   * Redirects to another URL.
   *
   * @param url The URL to redirect to
   */
  def Redirect(url : String) =
  {
    JsRaw("window.location.href = '" + url + "';").cmd
  }

  /**
   * Reloads the current page.
   */
  def Reload = JsRaw("window.location.reload();").cmd

  /**
   * Shows message box to the user.
   */
  def Message(msg : String) = JsRaw("alert('" + msg + "');").cmd

  /**
   * Tries to execute a function and shows an error box to the user in case it fails with an exception.
   *
   * @param description A short description of the function to be shown to the user e.g. "show links"
   */
  def Try(description : String = "")(func : => JsCmd) : JsCmd =
  {
    try
    {
      func
    }
    catch
    {
      case ex : Exception =>
      {
        logger.log(Level.INFO, "Error while trying to " + description, ex)
        Message("Error while trying to " + description + ". Details: " + ex.getMessage.encJs)
      }
    }
  }

  /**
   * Empty command which does nothing.
   */
  def Empty = JsRaw("").cmd

}
