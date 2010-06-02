package de.fuberlin.wiwiss.silk.metric

import de.fuberlin.wiwiss.silk.Instance
import de.fuberlin.wiwiss.silk.linkspec.{AnyParam, Metric}
import de.fuberlin.wiwiss.silk.metric._

class JaroWinklerMetric(val weight: Int, val params: Map[String, AnyParam]) extends Metric
{
    require(params.contains("str1"), "Parameter 'str1' is required")
    require(params.contains("str2"), "Parameter 'str2' is required")

    // maximum prefix length to use
    private final val MINPREFIXTESTLENGTH: Int = 6

    // prefix adjustment scale
    private final val PREFIXADUSTMENTSCALE: Double = 0.1

    override def evaluate(instance1: Instance, instance2: Instance) =
    {
        val set1 = params("str1").evaluate(instance1, instance2)
        val set2 = params("str2").evaluate(instance1, instance2)
        for (str1 <- set1; str2 <- set2) yield
        {
            jaroWinkler(str1, str2)
        }
    }

    /**
     * gets the similarity measure of the JaroWinkler metric for the given strings.
     *
     * @param string1
     * @param string2
     * @return 0 -1 similarity measure of the JaroWinkler metric
     */
    private def jaroWinkler(string1: String, string2: String): Double =
    {
        val dist = JaroDinstanceMetric.jaro(string1, string2)
        val prefixLength: Int = getPrefixLength(string1, string2)
        dist + (prefixLength.asInstanceOf[Double] * PREFIXADUSTMENTSCALE * (1.0 - dist))
    }

    /**
     * gets the prefix length found of common characters at the begining of the strings.
     *
     * @param string1
     * @param string2
     * @return the prefix length found of common characters at the begining of the strings
     */
    private def getPrefixLength(string1: String, string2: String): Int = {
        val max: Int = Math.min(MINPREFIXTESTLENGTH, Math.min(string1.length, string2.length))

        var i: Int = 0
        while (i < max)
        {
            if (string1.charAt(i) != string2.charAt(i)) {
                return i
            }
            i += 1
        }
        max
    }
}