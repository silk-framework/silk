package de.fuberlin.wiwiss.silk.workbench.scripts

import de.fuberlin.wiwiss.silk.workbench.scripts.ExperimentResult._

case class ExperimentResult(tables: Seq[Table]) {
  
  def toCsv = tables.map(_.toCsv).mkString("\n")
}

object ExperimentResult {

  case class Table(name: String, header: Seq[String], rows: Seq[Row]) {
    def toCsv = name + ":\nConfiguration," + header.mkString(",") + "\n" +  rows.map(_.toCsv).mkString("\n")
  }

  case class Row(name: String, values: Seq[Double]) {
    def toCsv = name + "," + values.mkString(",")
  }
}