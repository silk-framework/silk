package de.fuberlin.wiwiss.silk.learning

case class ResultTable(header: Seq[String], values: Seq[Seq[String]]) {

  def toCsv = {
    header.mkString("", ",", "\n") + values.map(_.mkString("", ",", "\n")).mkString
  }

  def toLatex = {
    "\\begin{tabular}{| l | l | c | c | c | c |}\n" +
    "\\hline\n" +
    header.mkString("", " & ", "\\\\\n") +
    "\\hline\n" +
    values.map(_.mkString("", " & ", "\\\\\n")).mkString +
    "\\hline\n" +
    "\\end{tabular}\n"
  }
}