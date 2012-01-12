package de.fuberlin.wiwiss.silk.workbench.scripts

case class MultipleTables(tables: Seq[Table]) {
  
  def transpose = MultipleTables(tables.map(_.transpose))
  
  def toCsv = tables.map(_.toCsv).mkString("\n\n")

  def toLatex = tables.map(_.toLatex).mkString("\n\n")
}

object MultipleTables {

  def build(metrics: Seq[PerformanceMetric], header: Seq[String], rowLabels: Seq[String], values: Seq[Seq[RunResult]]) = {
    MultipleTables(
      for(metric <- metrics) yield {
        val rows = for(v <- values) yield v.map(metric)
        Table(metric.name, header, rowLabels, rows)
      }
    )
  }
}