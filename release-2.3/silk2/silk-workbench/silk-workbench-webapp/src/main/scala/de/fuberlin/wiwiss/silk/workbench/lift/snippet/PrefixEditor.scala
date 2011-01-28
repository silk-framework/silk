package de.fuberlin.wiwiss.silk.workbench.lift.snippet

import xml.NodeSeq
import net.liftweb.http.SHtml
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JE.JsRaw

object PrefixEditor
{
  def readPrefixes(f : Map[String, String] => JsCmd) : JsCmd =
  {
    def update(str : String) =
    {
      val prefixes = for(Array(prefix, namespace) <- str.split(',').grouped(2)) yield (prefix, namespace)

      f(prefixes.toMap)
    }

    SHtml.ajaxCall(JsRaw("$('#prefixTable tr td input').toArray().map(function (a) { return a.value; })"), update)._2.cmd
  }

  def prefixEditor(prefixes : Map[String, String] = Map.empty) : NodeSeq =
  {
    def addRow() =
    {
      JsRaw("$('#prefixTable').append(\"<tr><td><input type='text' /></td><td><input type='text' size='50' /></td></tr>\");").cmd
    }

    def removeRow() =
    {
      JsRaw("$('#prefixTable tr td').parent().last().remove();").cmd
    }

    <p>
    <table id="prefixTable">
      <tr>
        <th>Prefix</th>
        <th>Namespace</th>
      </tr>
      {
        for((prefix, namespace) <- prefixes) yield
        {
          <tr>
            <td><input type='text' value={prefix} /></td>
            <td><input type='text' value={namespace} size="50" /></td>
          </tr>
        }
      }
    </table>
    {SHtml.ajaxButton("add", addRow _)}
    {SHtml.ajaxButton("remove", removeRow _)}
    </p>
  }
}