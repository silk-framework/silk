package de.fuberlin.wiwiss.silk.workbench.scripts

case class MultipleTables(tables: Seq[Table]) {
  
  def toCsv = tables.map(_.toCsv).mkString("\n\n")

  def toLatex = tables.map(_.toLatex).mkString("\n\n")
}

object MultipleTables {

  def build(metrics: Seq[PerformanceMetric], values: Seq[Seq[RunResult]], header: Seq[String], rowLabels: Seq[String]) = {
    MultipleTables(
      for(metric <- metrics) yield {
        val rows = for(v <- values) yield v.map(metric)
        Table(metric.name, header, rowLabels, rows)
      }
    )
  }
}