/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.silkframework.rule.plugins.distance.numeric

import java.util.logging.Logger

import org.silkframework.entity.Index
import org.silkframework.rule.similarity.SimpleDistanceMeasure
import org.silkframework.runtime.plugin.annotations.Plugin
import org.silkframework.util.StringUtils._

import scala.math._

@Plugin(
  id = "num",
  categories = Array("Numeric"),
  label = "Numeric similarity",
  description = "Computes the numeric distance between two numbers."
)
case class NumMetric(minValue: Double = Double.NegativeInfinity, maxValue: Double = Double.PositiveInfinity) extends SimpleDistanceMeasure {

  @transient
  private val logger = Logger.getLogger(classOf[NumMetric].getName)

  private val indexEnabled = {
    if (minValue.isNegInfinity || maxValue.isPosInfinity) {
      logger.info("Blocking disabled for numeric comparison as minValue and maxValue is not defined")
      false
    }
    else {
      true
    }
  }

  override def evaluate(str1: String, str2: String, limit: Double) = {
    (str1, str2) match {
      case (DoubleLiteral(num1), DoubleLiteral(num2)) => {
        abs(num1 - num2)
      }
      case _ => Double.PositiveInfinity
    }
  }

  override def emptyIndex(limit: Double): Index = {
    if(indexEnabled) {
      Index.continuousEmpty(minValue, maxValue, limit)
    } else {
      Index.empty
    }
  }

  override def indexValue(str: String, limit: Double, sourceOrTarget: Boolean): Index = {
    if (indexEnabled) {
      str match {
        case DoubleLiteral(num) => Index.continuous(num, minValue, maxValue, limit)
        case _ => emptyIndex(limit)
      }
    }
    else {
      Index.default
    }
  }
}
