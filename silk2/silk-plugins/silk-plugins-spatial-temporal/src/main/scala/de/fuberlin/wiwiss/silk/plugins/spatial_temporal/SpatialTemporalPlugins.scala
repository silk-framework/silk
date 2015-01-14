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

package de.fuberlin.wiwiss.silk.plugins.spatial_temporal

import java.util.logging.{Logger, Level}
import de.fuberlin.wiwiss.silk.linkagerule.input.Transformer
import de.fuberlin.wiwiss.silk.linkagerule.similarity.{DistanceMeasure}

import de.fuberlin.wiwiss.silk.plugins.spatial.distance._
import de.fuberlin.wiwiss.silk.plugins.temporal.distance._
import de.fuberlin.wiwiss.silk.plugins.spatial.transformer._




object SpatialTemporalPlugins {

  private val logger = Logger.getLogger("SpatialTemporalPlugins")

  def register() {
    logger.log(Level.FINE, "Registering Spatial-Temporal plugins.")

    //Spatial Transformers
    Transformer.register(classOf[GeometryTransformer])
    Transformer.register(classOf[PointsToCentroidTransformer])
    Transformer.register(classOf[SimplifyTransformer])
    Transformer.register(classOf[EnvelopeTransformer])
    Transformer.register(classOf[AreaTransformer])
    Transformer.register(classOf[BufferTransformer])

    //Spatial and Temporal Distance Metrics
    DistanceMeasure.register(classOf[CentroidDistanceMetric])
    DistanceMeasure.register(classOf[MinDistanceMetric])
    DistanceMeasure.register(classOf[de.fuberlin.wiwiss.silk.plugins.spatial.distance.ContainsMetric])
    DistanceMeasure.register(classOf[CrossesMetric])
    DistanceMeasure.register(classOf[DisjointMetric])
    DistanceMeasure.register(classOf[de.fuberlin.wiwiss.silk.plugins.spatial.distance.EqualsMetric])
    DistanceMeasure.register(classOf[IntersectsMetric])
    DistanceMeasure.register(classOf[de.fuberlin.wiwiss.silk.plugins.spatial.distance.OverlapsMetric])
    DistanceMeasure.register(classOf[TouchesMetric])
    DistanceMeasure.register(classOf[WithinMetric])
    DistanceMeasure.register(classOf[RelateMetric])
    DistanceMeasure.register(classOf[MillisecsDistanceMetric])
    DistanceMeasure.register(classOf[SecsDistanceMetric])
    DistanceMeasure.register(classOf[MinsDistanceMetric])
    DistanceMeasure.register(classOf[HoursDistanceMetric])
    DistanceMeasure.register(classOf[DaysDistanceMetric])
    DistanceMeasure.register(classOf[MonthsDistanceMetric])
    DistanceMeasure.register(classOf[YearsDistanceMetric])
    DistanceMeasure.register(classOf[BeforeMetric])
    DistanceMeasure.register(classOf[AfterMetric])
    DistanceMeasure.register(classOf[MeetsMetric])
    DistanceMeasure.register(classOf[IsMetByMetric])
    DistanceMeasure.register(classOf[de.fuberlin.wiwiss.silk.plugins.temporal.distance.OverlapsMetric])
    DistanceMeasure.register(classOf[IsOverlappedByMetric])
    DistanceMeasure.register(classOf[FinishesMetric])
    DistanceMeasure.register(classOf[IsFinishedByMetric])
    DistanceMeasure.register(classOf[de.fuberlin.wiwiss.silk.plugins.temporal.distance.ContainsMetric])
    DistanceMeasure.register(classOf[DuringMetric])
    DistanceMeasure.register(classOf[StartsMetric])
    DistanceMeasure.register(classOf[IsStartedByMetric])
    DistanceMeasure.register(classOf[de.fuberlin.wiwiss.silk.plugins.temporal.distance.EqualsMetric])   
  }
}