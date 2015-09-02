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

package de.fuberlin.wiwiss.silk.plugins.temporal

import java.util.logging.{Logger, Level}
import de.fuberlin.wiwiss.silk.rule.similarity.{DistanceMeasure}

import de.fuberlin.wiwiss.silk.plugins.temporal.distance._
import de.fuberlin.wiwiss.silk.plugins.temporal.relation._

/**
 * Register Temporal Plugins.
 * @author Panayiotis Smeros <psmeros@di.uoa.gr> (National and Kapodistrian University of Athens)
 */

object TemporalPlugins {

  private val logger = Logger.getLogger("TemporalPlugins")

  def register() {
    logger.log(Level.FINE, "Registering Temporal plugins.")

    //Temporal Distance Metrics
    DistanceMeasure.register(classOf[MillisecsDistanceMetric])
    DistanceMeasure.register(classOf[SecsDistanceMetric])
    DistanceMeasure.register(classOf[MinsDistanceMetric])
    DistanceMeasure.register(classOf[HoursDistanceMetric])
    DistanceMeasure.register(classOf[DaysDistanceMetric])
    DistanceMeasure.register(classOf[MonthsDistanceMetric])
    DistanceMeasure.register(classOf[YearsDistanceMetric])
    
    //Temporal Relations
    DistanceMeasure.register(classOf[BeforeMetric])
    DistanceMeasure.register(classOf[AfterMetric])
    DistanceMeasure.register(classOf[MeetsMetric])
    DistanceMeasure.register(classOf[IsMetByMetric])
    DistanceMeasure.register(classOf[OverlapsMetric])
    DistanceMeasure.register(classOf[IsOverlappedByMetric])
    DistanceMeasure.register(classOf[FinishesMetric])
    DistanceMeasure.register(classOf[IsFinishedByMetric])
    DistanceMeasure.register(classOf[ContainsMetric])
    DistanceMeasure.register(classOf[DuringMetric])
    DistanceMeasure.register(classOf[StartsMetric])
    DistanceMeasure.register(classOf[IsStartedByMetric])
    DistanceMeasure.register(classOf[EqualsMetric])   
  }
}