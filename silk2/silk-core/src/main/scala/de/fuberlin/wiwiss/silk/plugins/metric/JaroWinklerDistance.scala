package de.fuberlin.wiwiss.silk.plugins.metric

import de.fuberlin.wiwiss.silk.linkagerule.similarity.SimpleDistanceMeasure
import de.fuberlin.wiwiss.silk.util.plugin.Plugin

@Plugin(id = "jaroWinkler", label = "Jaro-Winkler distance", description = "String similarity based on the Jaro-Winkler metric.")
case class JaroWinklerDistance() extends SimpleDistanceMeasure {
  // maximum prefix length to use
  private final val MINPREFIXTESTLENGTH: Int = 4 //using value from lingpipe (was 6)

  // prefix adjustment scale
  private final val PREFIXADUSTMENTSCALE: Double = 0.1

  override def evaluate(str1: String, str2: String, threshold: Double) = {
    evaluateDistance(str1, str2)
  }

  /**
   * gets the similarity measure of the JaroWinkler metric for the given strings.
   *
   * @param string1
   * @param string2
   * @return 0 -1 similarity measure of the JaroWinkler metric
   */
  def evaluateDistance(string1: String, string2: String): Double = {
    val dist = JaroDinstanceMetric.jaro(string1, string2)
    val prefixLength = getPrefixLength(string1, string2)
    dist - (prefixLength.toDouble * PREFIXADUSTMENTSCALE * dist)
  }

  /**
   * gets the prefix length found of common characters at the begining of the strings.
   *
   * @param string1
   * @param string2
   * @return the prefix length found of common characters at the begining of the strings
   */
  private def getPrefixLength(string1: String, string2: String): Int = {
    val max: Int = math.min(MINPREFIXTESTLENGTH, math.min(string1.length, string2.length))

    var i: Int = 0
    while (i < max) {
      if (string1.charAt(i) != string2.charAt(i)) {
        return i
      }
      i += 1
    }
    max
  }
}