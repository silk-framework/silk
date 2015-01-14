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

package de.fuberlin.wiwiss.silk.plugins.spatial

import java.util.logging.{Logger, Level}
import de.fuberlin.wiwiss.silk.linkagerule.input.Transformer
import de.fuberlin.wiwiss.silk.linkagerule.similarity.{DistanceMeasure}

import de.fuberlin.wiwiss.silk.plugins.spatial.transformer._
import de.fuberlin.wiwiss.silk.plugins.spatial.distance._
import de.fuberlin.wiwiss.silk.plugins.spatial.relation._

/**
 * Register Spatial Plugins.
 * @author Panayiotis Smeros <psmeros@di.uoa.gr> (National and Kapodistrian University of Athens)
 */

object SpatialPlugins {

  private val logger = Logger.getLogger("SpatialPlugins")

  def register() {
    logger.log(Level.FINE, "Registering Spatial plugins.")

    //Spatial Transformers
    Transformer.register(classOf[GeometryTransformer])
    Transformer.register(classOf[PointsToCentroidTransformer])
    Transformer.register(classOf[SimplifyTransformer])
    Transformer.register(classOf[EnvelopeTransformer])
    Transformer.register(classOf[AreaTransformer])
    Transformer.register(classOf[BufferTransformer])

    //Spatial Distance Metrics
    DistanceMeasure.register(classOf[CentroidDistanceMetric])
    DistanceMeasure.register(classOf[MinDistanceMetric])
    
    //Spatial Relations
    DistanceMeasure.register(classOf[ContainsMetric])
    DistanceMeasure.register(classOf[CrossesMetric])
    DistanceMeasure.register(classOf[DisjointMetric])
    DistanceMeasure.register(classOf[EqualsMetric])
    DistanceMeasure.register(classOf[IntersectsMetric])
    DistanceMeasure.register(classOf[OverlapsMetric])
    DistanceMeasure.register(classOf[TouchesMetric])
    DistanceMeasure.register(classOf[WithinMetric])
    DistanceMeasure.register(classOf[RelateMetric])  
  }
}