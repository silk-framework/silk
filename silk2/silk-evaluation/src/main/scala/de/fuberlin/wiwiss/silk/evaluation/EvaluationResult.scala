package de.fuberlin.wiwiss.silk.evaluation

/**
 * TODO uses terms from information retrieval
 */
//TODO add Matthews_correlation_coefficient
class EvaluationResult(val truePositives : Int, val trueNegatives : Int,
                       val falsePositives : Int, val falseNegatives : Int)
{

  def score : Double = fMeasure

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
  
  override def toString = "(precision=" + precision + ", recall=" + recall + ", f-measure=" + fMeasure + ")"
}