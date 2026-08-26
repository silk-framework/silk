package org.silkframework.util

import scala.reflect.ClassTag
import scala.util.Random
import scala.collection.immutable.ArraySeq

/**
 * Created by andreas on 1/12/16.
 *
 * Utility methods related to sampling.
 */
object SampleUtil {

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
  def sample[T](values: Iterator[T],
                size: Int,
                filterOpt: Option[T => Boolean])
               (implicit m: ClassTag[T], random: Random): Seq[T] = {
    if(size <= 0) {
      return Seq.empty
    }
    val sample = new Array[T](size)

    var valueCount = 0L
    // Filter function for values
    val f: T => Boolean = filterOpt match {
      case Some(filter) => filter
      case None => t => true
    }

    for (value <- values if f(value)) {
      if (valueCount < size) {
        sample(valueCount.toInt) = value
      } else {
        // Replace a random position of the sample, with a probability that decreases as more values are seen.
        // Picking the position at random is what makes every value equally likely to end up in the sample.
        val index = (random.nextDouble() * (valueCount + 1)).toLong
        if (index < size) {
          sample(index.toInt) = value
        }
      }
      valueCount += 1
    }
    // Allow to return samples smaller than size
    ArraySeq.unsafeWrapArray(sample).take(math.min(size, valueCount).toInt)
  }
}
