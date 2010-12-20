package de.fuberlin.wiwiss.silk.workbench.lift.snippet

import xml.NodeSeq
import net.liftweb.http.SHtml
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JE.JsRaw

object PrefixEditor
{
  def readPrefixes(f : Map[String, String] => Unit) : JsCmd =
  {
    def update(str : String) =
    {
      val prefixes = for(Array(prefix, namespace) <- str.split(',').grouped(2)) yield (prefix, namespace)

      f(prefixes.toMap)

      JsRaw("").cmd
    }

    SHtml.ajaxCall(JsRaw("$('#prefixTable tr td input').toArray().map(function (a) { return a.value; })"), update)._2.cmd
  }

  def prefixEditor(prefixes : Map[String, String] = Map.empty) : NodeSeq =
  {
    def addRow() =
    {
      JsRaw("$('#prefixTable').append(\"<tr><td><input type='text' /></td><td><input type='text' /></td></tr>\");").cmd
    }

    <table id="prefixTable">
      <tr>
        <th>Prefix</th>
        <th>Namespace</th>
      </tr>
      {
        for((prefix, namespace) <- prefixes) yield
        {
          <tr>
            <td>{prefix}</td>
            <td>{namespace}</td>
          </tr>
        }
      }
      <tr>
        <td></td>
        <td>{SHtml.ajaxButton("add", addRow _)}</td>
      </tr>
    </table>
  }
}