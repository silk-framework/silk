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

import de.fuberlin.wiwiss.silk.plugins.transformer.spatial.GeometryTransformer
import de.fuberlin.wiwiss.silk.plugins.distance.spatial.CentroidDistanceMetric
import de.fuberlin.wiwiss.silk.plugins.distance.spatial.MinDistanceMetric
import de.fuberlin.wiwiss.silk.plugins.distance.spatial.ContainsMetric
import de.fuberlin.wiwiss.silk.plugins.distance.spatial.CrossesMetric
import de.fuberlin.wiwiss.silk.plugins.distance.spatial.DisjointMetric
import de.fuberlin.wiwiss.silk.plugins.distance.spatial.EqualsMetric
import de.fuberlin.wiwiss.silk.plugins.distance.spatial.IntersectsMetric
import de.fuberlin.wiwiss.silk.plugins.distance.spatial.OverlapsMetric
import de.fuberlin.wiwiss.silk.plugins.distance.spatial.TouchesMetric
import de.fuberlin.wiwiss.silk.plugins.distance.spatial.WithinMetric
import de.fuberlin.wiwiss.silk.plugins.distance.spatial.CrossesMetric
import de.fuberlin.wiwiss.silk.plugins.distance.spatial.DisjointMetric
import de.fuberlin.wiwiss.silk.plugins.distance.spatial.EqualsMetric
import de.fuberlin.wiwiss.silk.plugins.distance.spatial.IntersectsMetric
import de.fuberlin.wiwiss.silk.plugins.distance.spatial.OverlapsMetric
import de.fuberlin.wiwiss.silk.plugins.distance.spatial.TouchesMetric
import de.fuberlin.wiwiss.silk.plugins.distance.spatial.WithinMetric
import de.fuberlin.wiwiss.silk.plugins.distance.spatial.RelateMetric
import de.fuberlin.wiwiss.silk.plugins.transformer.spatial.PointsToCentroidTransformer
import de.fuberlin.wiwiss.silk.plugins.transformer.spatial.SimplifyTransformer
import de.fuberlin.wiwiss.silk.plugins.transformer.spatial.EnvelopeTransformer
import de.fuberlin.wiwiss.silk.plugins.transformer.spatial.AreaTransformer
import de.fuberlin.wiwiss.silk.plugins.transformer.spatial.BufferTransformer
import de.fuberlin.wiwiss.silk.plugins.distance.temporal.MillisecsDistanceMetric
import de.fuberlin.wiwiss.silk.plugins.distance.temporal.SecsDistanceMetric
import de.fuberlin.wiwiss.silk.plugins.distance.temporal.MinsDistanceMetric
import de.fuberlin.wiwiss.silk.plugins.distance.temporal.HoursDistanceMetric
import de.fuberlin.wiwiss.silk.plugins.distance.temporal.DaysDistanceMetric
import de.fuberlin.wiwiss.silk.plugins.distance.temporal.MonthsDistanceMetric
import de.fuberlin.wiwiss.silk.plugins.distance.temporal.YearsDistanceMetric
import de.fuberlin.wiwiss.silk.plugins.distance.temporal.BeforeMetric
import de.fuberlin.wiwiss.silk.plugins.distance.temporal.AfterMetric
import de.fuberlin.wiwiss.silk.plugins.distance.temporal.MeetsMetric
import de.fuberlin.wiwiss.silk.plugins.distance.temporal.IsMetByMetric
import de.fuberlin.wiwiss.silk.plugins.distance.temporal.OverlapsMetric
import de.fuberlin.wiwiss.silk.plugins.distance.temporal.IsOverlappedByMetric
import de.fuberlin.wiwiss.silk.plugins.distance.temporal.FinishesMetric
import de.fuberlin.wiwiss.silk.plugins.distance.temporal.IsFinishedByMetric
import de.fuberlin.wiwiss.silk.plugins.distance.temporal.ContainsMetric
import de.fuberlin.wiwiss.silk.plugins.distance.temporal.DuringMetric
import de.fuberlin.wiwiss.silk.plugins.distance.temporal.StartsMetric
import de.fuberlin.wiwiss.silk.plugins.distance.temporal.IsStartedByMetric
import de.fuberlin.wiwiss.silk.plugins.distance.temporal.EqualsMetric


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
    DistanceMeasure.register(classOf[de.fuberlin.wiwiss.silk.plugins.distance.spatial.ContainsMetric])
    DistanceMeasure.register(classOf[CrossesMetric])
    DistanceMeasure.register(classOf[DisjointMetric])
    DistanceMeasure.register(classOf[de.fuberlin.wiwiss.silk.plugins.distance.spatial.EqualsMetric])
    DistanceMeasure.register(classOf[IntersectsMetric])
    DistanceMeasure.register(classOf[de.fuberlin.wiwiss.silk.plugins.distance.spatial.OverlapsMetric])
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
    DistanceMeasure.register(classOf[de.fuberlin.wiwiss.silk.plugins.distance.temporal.OverlapsMetric])
    DistanceMeasure.register(classOf[IsOverlappedByMetric])
    DistanceMeasure.register(classOf[FinishesMetric])
    DistanceMeasure.register(classOf[IsFinishedByMetric])
    DistanceMeasure.register(classOf[de.fuberlin.wiwiss.silk.plugins.distance.temporal.ContainsMetric])
    DistanceMeasure.register(classOf[DuringMetric])
    DistanceMeasure.register(classOf[StartsMetric])
    DistanceMeasure.register(classOf[IsStartedByMetric])
    DistanceMeasure.register(classOf[de.fuberlin.wiwiss.silk.plugins.distance.temporal.EqualsMetric])   
  }
}