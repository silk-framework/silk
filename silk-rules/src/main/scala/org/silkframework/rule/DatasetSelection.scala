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

package org.silkframework.rule

import org.silkframework.config.Prefixes
import org.silkframework.entity.Restriction
import org.silkframework.rule.task.DatasetOrTransformTaskAutoCompletionProvider
import org.silkframework.runtime.plugin.{PluginObjectParameter, ReferencePluginParameterAutoCompletionProvider}
import org.silkframework.runtime.plugin.annotations.{Param, Plugin}
import org.silkframework.util.{Identifier, Uri}
import org.silkframework.workspace.project.task.DatasetTaskReferenceAutoCompletionProvider

import scala.xml.{Elem, Node}

/**
 * Defines a dataset.
 *
 * @param inputId The id of the dataset or transformation to be used for retrieving entities.
 * @param restriction Restricts this dataset to specific resources.
 */
@Plugin(
  id = "datasetSelectionParameter",
  label = "Dataset Selection",
  description = "Select the set of input instances, defined by the data source, the type of the instances and an optional restriction pattern to" +
      " further restrict the selected instances."
) // TODO: Better name, since we select a set of entities, not just a dataset
case class DatasetSelection(@Param(label = "Dataset", value = "The dataset to select.",
                            autoCompletionProvider = classOf[DatasetOrTransformTaskAutoCompletionProvider],
                                   autoCompleteValueWithLabels = true, allowOnlyAutoCompletedValues = true)
                            inputId: Identifier,
                            @Param(label = "Type", value = "The type of the dataset. If left empty, the default type will be selected.",
                              autoCompletionProvider = classOf[DatasetTypeAutoCompletionProviderReference], autoCompletionDependsOnParameters = Array("inputId"))
                            typeUri: Uri,
                            @Param(label = "Restriction", value = "Additional restrictions on the enumerated entities. If this is an RDF source, " +
                                    "use SPARQL patterns that include the variable ?a to identify the enumerated entities, e.g. ?a foaf:knows <http://example.org/SomePerson>")
                            restriction: Restriction = Restriction.empty) extends PluginObjectParameter {

  /**
   * Serializes this dataset specification as XML.
   *
   * @param asSource If true, this dataset will be serialized as a source dataset. If false it will be serialize as target dataset.
   */
  def toXML(asSource: Boolean): Elem = {
    if (asSource) {
      <SourceDataset dataSource={inputId} var="a" typeUri={typeUri.uri}>
        <RestrictTo>
          {restriction.serialize}
        </RestrictTo>
      </SourceDataset>
    }
    else {
      <TargetDataset dataSource={inputId} var="b" typeUri={typeUri.uri}>
        <RestrictTo>
          {restriction.serialize}
        </RestrictTo>
      </TargetDataset>
    }
  }
}

object DatasetSelection {
  /**
   * Creates a DatasetSpecification from XML.
   */
  def fromXML(node: Node)(implicit prefixes: Prefixes = Prefixes.empty): DatasetSelection = {
    var restrictionText = (node \ "RestrictTo").text.trim
    // Currently the entity is always expected to be referenced with the variable ?a.
    val variable = (node \ "@var").text
    restrictionText = restrictionText.replace("?" + variable, "?a")

    val typeUri = (node \ "@typeUri").text

    DatasetSelection(
      inputId = (node \ "@dataSource").text,
      typeUri = Uri(typeUri),
      restriction = Restriction.parse(restrictionText)
    )
  }

  def empty = DatasetSelection("EmptyDatasetSelection", Uri(""), Restriction.empty)


}


case class DatasetTypeAutoCompletionProviderReference() extends ReferencePluginParameterAutoCompletionProvider {
  override def pluginParameterAutoCompletionProviderId: String = "datasetTypeAutoCompletionProvider"
}