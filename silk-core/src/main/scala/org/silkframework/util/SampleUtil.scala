package org.silkframework.util

import scala.reflect.ClassTag
import scala.util.Random

/**
 * Created by andreas on 1/12/16.
 *
 * Utility methods related to sampling.
 */
object SampleUtil {
  private val r = new Random()

  /**
   * Sample a fixed size sample set from a set larger than the target set uniformly.
   * This algorithm is a generalization to multiple values of the one explained in this [[http://jeremykun.com/2013/07/05/reservoir-sampling/ Reservoir Sampling]] article.
   *
   * The algorithm uses a fixed memory size that is proportional to the requested sample size.
   *
   * @param values input set of values
   * @param size Target (max) size of the sampled set. If the input set is smaller then the target set will have the size
   *             of the input set.
   * @param filterOpt Function that returns true if the entity should be kept or false if it should be filtered out.
   * @tparam T
   * @return
   */
  def sample[T](values: Traversable[T],
                size: Int,
                filterOpt: Option[T => Boolean])
               (implicit m: ClassTag[T]): Seq[T] = {
    val sample = new Array[T](size)

    var valueCount = 0l
    // Init first round
    var step = 1
    var nextSampleProbability = 1.0 / step
    // Filter function for values
    val f: T => Boolean = filterOpt match {
      case Some(filter) => filter
      case None => t => true
    }

    for (value <- values if f(value)) {
      if (valueCount < size) {
        sample(valueCount.toInt) = value
      } else if (r.nextDouble() < nextSampleProbability) {
        val idx = (valueCount % size).toInt // Round-robin over each position in the array
        sample(idx) = value
      }
      valueCount += 1
      if (valueCount % size == 0) {
        // Init next round: A new round begins when all positions have been covered with the current probability
        step += 1
        nextSampleProbability = 1.0 / step
      }
    }
    // Allow to return samples smaller than size
    sample.take(math.min(size, valueCount).toInt)
  }
}