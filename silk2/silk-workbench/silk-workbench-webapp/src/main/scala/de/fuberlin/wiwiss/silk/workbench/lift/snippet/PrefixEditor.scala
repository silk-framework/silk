package de.fuberlin.wiwiss.silk.workbench.lift.snippet

import xml.NodeSeq
import net.liftweb.http.SHtml
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JE.JsRaw
import de.fuberlin.wiwiss.silk.config.Prefixes
import java.util.concurrent.atomic.AtomicInteger

class PrefixEditor
{
  private var id = "prefixTable" + PrefixEditor.lastID.incrementAndGet()

  def read(f : Prefixes => JsCmd) : JsCmd =
  {
    def update(str : String) =
    {
      val prefixes = for(Array(prefix, namespace) <- str.split(',').grouped(2)) yield (prefix, namespace)

      f(Prefixes(prefixes.toMap))
    }

    SHtml.ajaxCall(JsRaw("$('#" + id + " tr td input').toArray().map(function (a) { return a.value; })"), update)._2.cmd
  }

  def show(prefixes : Map[String, String] = Map.empty) : NodeSeq =
  {
    def addRow() =
    {
      JsRaw("$('#" + id + "').append(\"<tr><td><input type='text' /></td><td><input type='text' size='50' /></td></tr>\");").cmd
    }

    def removeRow() =
    {
      JsRaw("$('#" + id + " tr td').parent().last().remove();").cmd
    }

    <p>
    <table id={id}>
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

object PrefixEditor
{
  private var lastID = new AtomicInteger(0)
}