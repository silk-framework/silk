package de.fuberlin.wiwiss.silk.learning

case class LearningStatistics(time: Long, iterations: Int, message: String)

object StatisticsLatexFormatter {
  def apply(statistics: Seq[LearningStatistics]): String = {
    val header =
      "\\begin{tabular}{| l | l | l | l |}\n" +
      "\\hline\n" +
      "Run & Iterations & Time & Result \\\\\n" +
      "\\hline\n"

    val rows = statistics.zipWithIndex.map(row _ tupled).mkString

    val footer =
      "\\hline\n" +
 	    "\\end{tabular}\n"

    header + rows + averageRow(statistics) + footer
  }

  private def row(statistics: LearningStatistics, run: Int): String = {
    run + " & " + statistics.iterations + " & " + (statistics.time / 1000.0) + "s & " + statistics.message + "\\\\\n"
  }

  private def averageRow(statistics: Seq[LearningStatistics]): String = {
    val averageIterations = statistics.map(_.iterations).sum.toDouble / statistics.size
    val averageTime = statistics.map(_.time / 1000.0).sum / statistics.size

    "\\hline\n" +
    "Average & " + averageIterations + " & " + averageTime + "s & \\\\\n"
  }
}