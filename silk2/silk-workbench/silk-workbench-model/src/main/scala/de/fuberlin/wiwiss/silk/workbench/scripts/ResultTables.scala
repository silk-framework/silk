package de.fuberlin.wiwiss.silk.workbench.scripts

import de.fuberlin.wiwiss.silk.workbench.scripts.ResultTables._

case class ResultTables(tables: Seq[Table]) {
  
  def toCsv = tables.map(_.toCsv).mkString("\n\n")
}

object ResultTables {

  def build(metrics: Seq[PerformanceMetric], values: Seq[Seq[RunResult]], columnLabels: Seq[String], rowLabels: Seq[String]) = {
    ResultTables(
      for(metric <- metrics) yield {
        val rows =
          for((c,v) <- rowLabels zip values) yield {
            Row(c, v.map(metric))
          }
        Table(metric.name, columnLabels, rows)
      }
    )
  }
  
  case class Table(name: String, header: Seq[String], rows: Seq[Row]) {
    def toCsv = name + ":\nConfiguration," + header.mkString(",") + "\n" +  rows.map(_.toCsv).mkString("\n")
  }

  case class Row(name: String, values: Seq[Double]) {
    def toCsv = name + "," + values.mkString(",")
  }
}