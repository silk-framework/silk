package de.fuberlin.wiwiss.silk.evaluation

import scala.math.sqrt

/**
 * TODO uses terms from information retrieval
 */
class EvaluationResult(val truePositives : Int, val trueNegatives : Int,
                       val falsePositives : Int, val falseNegatives : Int,
                       val score : Double)
{

  //def score : Double = score

  /**
   * The '''specificity''' or '''true negative rate (TNR)''' is the proportion of the links which have not been generated of the negative links in the alignment.
   */
  def specificity = trueNegatives.toDouble / (trueNegatives + falsePositives)

  /**
   * The '''recall''', '''sensitivity''' or '''true positive rate (TPR)''' is the proportion of the links which have been generated of the positive links in the alignment.
   */
  def recall = truePositives.toDouble / (truePositives + falseNegatives)

  /**
   * The '''precision''' or '''positive predictive value (PPV)''' is the proportion of positive links in the alignment which have been generated.
   */
  def precision = if(truePositives > 0) truePositives.toDouble / (truePositives + falsePositives) else 0.0

  /**
   * The harmonic mean of precision and recall.
   */
  def fMeasure = if(precision + recall > 0.0) 2.0 * precision * recall / (precision + recall) else 0.0

  /**
   * Matthews correlation coefficient.
   */
  def mcc
  {
    val cross = truePositives * trueNegatives - falsePositives * falseNegatives
    val sum = (truePositives + falsePositives) * (truePositives + falseNegatives ) * (trueNegatives + falsePositives) * (trueNegatives + falseNegatives)

    cross.toDouble / sqrt(sum.toDouble)
  }
  
  //override def toString = "(precision=" + precision + ", recall=" + recall + ", f-measure=" + fMeasure + ")"
  override def toString = "(score=" + score + ", f-measure=" + fMeasure + ")"
}