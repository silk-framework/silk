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
import de.fuberlin.wiwiss.silk.rule.input.Transformer
import de.fuberlin.wiwiss.silk.rule.similarity.{DistanceMeasure}

import de.fuberlin.wiwiss.silk.plugins.spatial.transformer._
import de.fuberlin.wiwiss.silk.plugins.spatial.distance._
import de.fuberlin.wiwiss.silk.plugins.spatial.relation._
import de.fuberlin.wiwiss.silk.runtime.plugin.PluginModule

/**
 * Register Spatial Plugins.
 * @author Panayiotis Smeros <psmeros@di.uoa.gr> (National and Kapodistrian University of Athens)
 */

class SpatialPlugins extends PluginModule {

  override def pluginClasses = spatialTransformers ::: spatialDistanceMetrics ::: spatialRelations

  private def spatialTransformers =
    classOf[GeometryTransformer] ::
    classOf[PointsToCentroidTransformer] ::
    classOf[SimplifyTransformer] ::
    classOf[EnvelopeTransformer] ::
    classOf[AreaTransformer] ::
    classOf[BufferTransformer] :: Nil

  private def spatialDistanceMetrics =
    classOf[CentroidDistanceMetric] ::
    classOf[MinDistanceMetric] :: Nil
    
  private def spatialRelations =
    classOf[ContainsMetric] ::
    classOf[CrossesMetric] ::
    classOf[DisjointMetric] ::
    classOf[EqualsMetric] ::
    classOf[IntersectsMetric] ::
    classOf[OverlapsMetric] ::
    classOf[TouchesMetric] ::
    classOf[WithinMetric] ::
    classOf[RelateMetric] :: Nil
}