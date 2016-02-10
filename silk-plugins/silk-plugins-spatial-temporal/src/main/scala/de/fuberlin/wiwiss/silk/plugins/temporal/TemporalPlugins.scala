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

package org.silkframework.plugins.temporal

import org.silkframework.plugins.temporal.distance._
import org.silkframework.plugins.temporal.relation._
import org.silkframework.runtime.plugin.PluginModule

/**
 * Register Temporal Plugins.
 * @author Panayiotis Smeros <psmeros@di.uoa.gr> (National and Kapodistrian University of Athens)
 */
class TemporalPlugins extends PluginModule {

  override def pluginClasses = temporalDistanceMetrics ::: temporalRelations

  private def temporalDistanceMetrics =
    classOf[MillisecsDistanceMetric] ::
    classOf[SecsDistanceMetric] ::
    classOf[MinsDistanceMetric] ::
    classOf[HoursDistanceMetric] ::
    classOf[DaysDistanceMetric] ::
    classOf[MonthsDistanceMetric] ::
    classOf[YearsDistanceMetric] :: Nil
    
  private def temporalRelations =
    classOf[BeforeMetric] ::
    classOf[AfterMetric] ::
    classOf[MeetsMetric] ::
    classOf[IsMetByMetric] ::
    classOf[OverlapsMetric] ::
    classOf[IsOverlappedByMetric] ::
    classOf[FinishesMetric] ::
    classOf[IsFinishedByMetric] ::
    classOf[ContainsMetric] ::
    classOf[DuringMetric] ::
    classOf[StartsMetric] ::
    classOf[IsStartedByMetric] ::
    classOf[EqualsMetric] :: Nil
}