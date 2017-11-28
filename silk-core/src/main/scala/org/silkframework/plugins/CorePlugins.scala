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

package org.silkframework.plugins

import org.silkframework.dataset.VariableDataset
import org.silkframework.dataset.DatasetSpec.{DatasetSpecFormat, DatasetTaskFormat}
import org.silkframework.entity.EntitySchema.EntitySchemaFormat
import org.silkframework.plugins.dataset.InternalDataset
import org.silkframework.runtime.plugin.PluginModule

import scala.language.existentials

/**
  * Registers all default plugins.
  */
class CorePlugins extends PluginModule {

  override def pluginClasses = datasets ++ serializers

  private def datasets =
    classOf[InternalDataset] ::
    classOf[VariableDataset] ::
    Nil

  private def serializers =
    DatasetSpecFormat.getClass ::
    DatasetTaskFormat.getClass ::
    EntitySchemaFormat.getClass :: Nil
}
