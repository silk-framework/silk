package de.fuberlin.wiwiss.silk.metric

import de.fuberlin.wiwiss.silk.linkspec.AnyParam
import de.fuberlin.wiwiss.silk.linkspec.Metric
import de.fuberlin.wiwiss.silk.Instance

class JaroDistanceMetric(val weight: Int, val params: Map[String, AnyParam]) extends Metric
{
    require(params.contains("str1"), "Parameter 'str1' is required")
    require(params.contains("str2"), "Parameter 'str2' is required")

    override def evaluate(instance1: Instance, instance2: Instance) =
    {
        val set1 = params("str1").evaluate(instance1, instance2)
        val set2 = params("str2").evaluate(instance1, instance2)
        for (str1 <- set1; str2 <- set2) yield
        {
            JaroDinstanceMetric.jaro(str1, str2)
        }
    }


}

object JaroDinstanceMetric
{
    def jaro(string1: String, string2: String): Double =
    {
        //get half the length of the string rounded up - (this is the distance used for acceptable transpositions)
        val halflen: Int = ((Math.min(string1.length, string2.length)) / 2) + ((Math.min(string1.length, string2.length)) % 2)

        //get common characters
        val common1: StringBuffer = getCommonCharacters(string1, string2, halflen)
        val common2: StringBuffer = getCommonCharacters(string2, string1, halflen)

        //check for zero in common
        if (common1.length == 0 || common2.length == 0) {
            return 0.0
        }

        //check for same length common strings returning 0.0 is not the same
        if (common1.length != common2.length) {
            return 0.0
        }

        //get the number of transpositions
        var transpositions: Int = 0

        for (i <- 0 to common1.length)
        {
            if (common1.charAt(i) != common2.charAt(i))
            {
                transpositions += 1
            }
        }

        transpositions = transpositions / 2

        //calculate jaro metric
        return (common1.length / (string1.length.asInstanceOf[Double]) + common2.length / (string2.length.asInstanceOf[Double]) + (common1.length - transpositions) / (common1.length.asInstanceOf[Double])) / 3.0
    }


    /**
     * returns a string buffer of characters from string1 within string2 if they are of a given
     * distance seperation from the position in string1.
     *
     * @param string1
     * @param string2
     * @param distanceSep
     * @return a string buffer of characters from string1 within string2 if they are of a given
     *         distance seperation from the position in string1
     */
    private def getCommonCharacters(string1: String, string2: String, distanceSep: Int): StringBuffer = {
        val returnCommons: StringBuffer = new StringBuffer
        val copy: StringBuffer = new StringBuffer(string2)

        var i = 0
        for (string1Char <- string1)
        {
            var foundIt: Boolean = false
            for (string2Char <- copy.toString)
            {
                var j: Int = Math.max(0, i - distanceSep)
                while (!foundIt && j < Math.min(i + distanceSep, string2.length - 1))
                {
                    if (copy.charAt(j) == string1Char)
                    {
                        foundIt = true
                        returnCommons.append(string1Char)
                        copy.setCharAt(j, '0')
                    }
                }
            }
            i += 1
        }
        returnCommons
    }
}