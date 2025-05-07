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

import org.silkframework.config.CustomTask.CustomTaskFormat
import org.silkframework.config.Task.GenericTaskFormat
import org.silkframework.config.TaskSpec.TaskSpecXmlFormat
import org.silkframework.dataset.DatasetSpec.{DatasetSpecFormat, DatasetTaskXmlFormat}
import org.silkframework.dataset.VariableDataset
import org.silkframework.dataset.operations.{AddProjectFilesOperator, DeleteFilesOperator, LocalAddProjectFilesOperatorExecutor, LocalDeleteFilesOperatorExecutor}
import org.silkframework.entity.EntitySchema.EntitySchemaFormat
import org.silkframework.entity.ValueType
import org.silkframework.execution.local.LocalExecutionManager
import org.silkframework.plugins.dataset.InternalDataset
import org.silkframework.runtime.plugin.{AnyPlugin, PluginModule}

import scala.language.existentials

/**
  * Registers all default plugins.
  */
class CorePlugins extends PluginModule {

  override def pluginClasses: Seq[Class[_ <: AnyPlugin]] = datasets ++ datasetOperations ++ serializers ++ valueTypes :+ classOf[LocalExecutionManager]

  private def datasets: Seq[Class[_ <: AnyPlugin]] =
    classOf[InternalDataset] ::
    classOf[VariableDataset] ::
    Nil

  private def datasetOperations: Seq[Class[_ <: AnyPlugin]] = {
    classOf[AddProjectFilesOperator] ::
    classOf[LocalAddProjectFilesOperatorExecutor] ::
    classOf[DeleteFilesOperator] ::
    classOf[LocalDeleteFilesOperatorExecutor] ::
    Nil
  }

  private def serializers: Seq[Class[_ <: AnyPlugin]] =
    TaskSpecXmlFormat.getClass ::
    GenericTaskFormat.getClass ::
    DatasetSpecFormat.getClass ::
    DatasetTaskXmlFormat.getClass ::
    CustomTaskFormat.getClass ::
    EntitySchemaFormat.getClass :: Nil

  private def valueTypes: Seq[Class[_ <: AnyPlugin]] = {
    ValueType.valueTypeIdMapByClass.keys.toSeq
  }
}
