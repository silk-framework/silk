package de.fuberlin.wiwiss.silk.plugins.metric.equality

import de.fuberlin.wiwiss.silk.linkagerule.similarity.SimpleDistanceMeasure
import java.util.Locale
import de.fuberlin.wiwiss.silk.entity.Index
import java.text.Collator
import de.fuberlin.wiwiss.silk.util.plugin.Plugin
import de.fuberlin.wiwiss.silk.util.plugin.Plugin._

/**
 * Equality Metric with some fuzziness regarding case, accents, diereses, etc.
 * For example, "uber", "Über", "ÜBER", and "ÜBÉR" will be considered equal.
 *
 * The metric is based on java.text.Collator for locale "en_US", with strength Collator.PRIMARY
 * and decomposition Collator.FULL_DECOMPOSITION.
 *
 * @author Florian Kleedorfer, Research Studios Austria
 */
@Plugin(id = "relaxedEquality", label = "RelaxedEquality", description = "Return 1 if strings are equal, 0 otherwise. Lower/upper case and differences like ö/o, n/ñ, c/ç etc. are treated as equal.")
class RelaxedEqualityMetric extends SimpleDistanceMeasure {

  val collator = {
    //Collator is locale-specific, but finding the actual differences was aborted without success after some time
    //If there are differences relevant to our purposes for a metric, it makes sense to make the locale a
    //parameter, but right now, it's better not to confuse anyone without a good reason and just use "en_US".
    //We're not using Locale.getDefault so as to yield reproducible results, just in case.
    val locale = new Locale("en_US")

    val newCollator = Collator.getInstance(locale)
    newCollator.setStrength(Collator.PRIMARY)
    newCollator.setDecomposition(Collator.FULL_DECOMPOSITION)
    newCollator
  }

  /**
   * uses the
   * @param value1
   * @param value2
   * @param limit
   * @return
   */
  def evaluate(value1: String, value2: String, limit: Double) = {
    if (collator.equals(value1, value2)) 0.0 else 1.0
  }

  override def indexValue(value: String, limit: Double) = {
    Index.oneDim(Set(collator.getCollationKey(value).hashCode()))
  }
}
