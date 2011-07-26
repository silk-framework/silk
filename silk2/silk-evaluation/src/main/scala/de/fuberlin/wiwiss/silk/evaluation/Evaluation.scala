package de.fuberlin.wiwiss.silk.evaluation

import java.io.File

object Evaluation {
  def main(args: Array[String]) {
    val evalFile = System.getProperty("evalFile") match {
      case fileName: String => new File(fileName)
      case _ => throw new IllegalArgumentException("No evaluation file specified. Please set the 'evalFile' property")
    }

    val referenceFile = System.getProperty("referenceFile") match {
      case fileName: String => new File(fileName)
      case _ => throw new IllegalArgumentException("No reference file specified. Please set the 'referenceFile' property")
    }

    val evalAlignment = AlignmentReader.read(evalFile)
    val referenceAlignment = AlignmentReader.read(referenceFile)

    //println("Evaluation aligments: " + evalAlignment.size)
    //println("Reference aligments: " + referenceAlignment.size)

    //println(evaluate(evalAlignment, referenceAlignment))
    //printDiff(evalAlignment, referenceAlignment)
  }

  /**
   * Evaluates a alignment against a reference alignment.
   */
  //    def evaluate(evalAlignment : Set[Link], referenceAlignment : Set[Link]) : EvaluationResult =
  //    {
  //        val intersectionSize = evalAlignment.intersect(referenceAlignment).size
  //        new EvaluationResult(intersectionSize / evalAlignment.size, intersectionSize / referenceAlignment.size)
  //    }

  /**
   * Prints the difference between an evaluation alignment and a reference alignment.
   */
  //    def printDiff(evalAlignment : Set[Link], referenceAlignment : Set[Link])
  //    {
  //        for(falseNegative <- referenceAlignment -- evalAlignment)
  //        {
  //            println("-" + falseNegative)
  //        }
  //
  //        for(falsePositive <- evalAlignment -- referenceAlignment)
  //        {
  //            println("+" + falsePositive)
  //        }
  //    }
}
