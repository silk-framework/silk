package de.fuberlin.wiwiss.silk.evaluation

import java.io.File

object Evaluation
{
    def main(args : Array[String])
    {
        val evalFile = System.getProperty("evalFile") match
        {
            case fileName : String => new File(fileName)
            case _ => throw new IllegalArgumentException("No evaluation file specified. Please set the 'evalFile' property")
        }

        val referenceFile = System.getProperty("referenceFile") match
        {
            case fileName : String => new File(fileName)
            case _ => throw new IllegalArgumentException("No reference file specified. Please set the 'referenceFile' property")
        }
        
        val evalAlignments = AlignmentReader.read(evalFile).toSet
        val referenceAlignments = AlignmentReader.read(referenceFile).toSet

        println(evaluate(evalAlignments, referenceAlignments))
        printDiff(evalAlignments, referenceAlignments)
    }

    /**
     * Evaluates a set of alignments against a reference set.
     */
    def evaluate(evalAlignments : Set[Alignment], referenceAlignments : Set[Alignment]) : EvaluationResult =
    {
        val intersectionSize = evalAlignments.intersect(referenceAlignments).size
        new EvaluationResult(intersectionSize / evalAlignments.size, intersectionSize / referenceAlignments.size)
    }

    /**
     * Prints the difference between an evaluation set and a reference set.
     */
    def printDiff(evalAlignments : Set[Alignment], referenceAlignments : Set[Alignment])
    {
        for(falseNegative <- referenceAlignments -- evalAlignments)
        {
            println("-" + falseNegative)
        }

        for(falsePositive <- evalAlignments -- referenceAlignments)
        {
            println("+" + falsePositive)
        }
    }
}
