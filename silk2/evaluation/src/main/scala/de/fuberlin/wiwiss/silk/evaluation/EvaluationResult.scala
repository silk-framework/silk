package de.fuberlin.wiwiss.silk.evaluation


class EvaluationResult(val precision : Double, val recall : Double)
{
    def fMeassure = 2.0 * precision * recall / (precision + recall)

    override def toString = "EvaluationResult(precision=" + precision + ", recall=" + recall + ")"
}
