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

package org.silkframework.learning.generation

import org.silkframework.entity.paths.UntypedPath
import org.silkframework.learning.LearningConfiguration.Components
import org.silkframework.learning.individual._
import org.silkframework.rule.input.Transformer
import org.silkframework.rule.similarity.DistanceMeasure
import org.silkframework.runtime.plugin.PluginContext
import org.silkframework.util.DPair

class PatternGenerator(components: Components)
                      (implicit pluginContext: PluginContext) {

  private val handlers = LabelHandler :: Wgs84Handler :: Nil

  def apply(paths: DPair[Traversable[UntypedPath]]): Traversable[ComparisonGenerator] = {
    handlers.flatMap(_.apply(paths))
  }

  private trait Handler extends (DPair[Traversable[UntypedPath]] => Option[ComparisonGenerator]) {
    protected def getProperty(paths: DPair[Traversable[UntypedPath]], property: String): Option[DPair[UntypedPath]] = {
      (paths.source.find(_.serialize().contains(property)), paths.target.find(_.serialize().contains(property))) match {
        case (Some(sourcePath), Some(targetPath)) => Some(DPair(sourcePath, targetPath))
        case _ => None
      }
    }
  }

  private object LabelHandler extends Handler {
    private val labelProperty = "http://www.w3.org/2000/01/rdf-schema#label"

    def apply(paths: DPair[Traversable[UntypedPath]]) = {
      getProperty(paths, labelProperty).map(createGenerator)
    }

    //TODO create measures other than levensthein
    private def createGenerator(pathPair: DPair[UntypedPath]) = {
      new ComparisonGenerator(
        inputGenerators = InputGenerator.fromPathPair(pathPair, components.transformations),
        measure = FunctionNode("levenshteinDistance", Nil, DistanceMeasure),
        maxThreshold = 2.0
      )
    }
  }

  private object Wgs84Handler extends Handler {
    private val latProperty = "http://www.w3.org/2003/01/geo/wgs84_pos#lat"
    private val longProperty = "http://www.w3.org/2003/01/geo/wgs84_pos#long"

    def apply(paths: DPair[Traversable[UntypedPath]]) = {
      (getProperty(paths, latProperty), getProperty(paths, longProperty)) match {
        case (Some(latPaths), Some(longPaths)) => Some(createGenerator(latPaths, longPaths))
        case _ => None
      }
    }

    private def createGenerator(latPaths: DPair[UntypedPath], longPaths: DPair[UntypedPath]) = {
      val inputs =
        DPair(
          TransformNode(
            isSource = true,
            inputs = PathInputNode(latPaths.source, true) :: PathInputNode(longPaths.source, true) :: Nil,
            transformer = FunctionNode("concat", ParameterNode("glue", " ") :: Nil, Transformer)
          ),
          TransformNode(
            isSource = false,
            inputs = PathInputNode(latPaths.target, false) :: PathInputNode(longPaths.target, false) :: Nil,
            transformer = FunctionNode("concat", ParameterNode("glue", " ") :: Nil, Transformer)
          )
        )

      new ComparisonGenerator(
        inputGenerators = InputGenerator.fromInputPair(inputs, components.transformations),
        measure = FunctionNode("wgs84", ParameterNode("unit", "km") :: Nil, DistanceMeasure),
        maxThreshold = 50.0
      )
    }
  }

}