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

import org.silkframework.entity.{Entity, Index}
import org.silkframework.rule.similarity.SimilarityOperator
import org.silkframework.runtime.plugin.PluginObjectParameterNoSchema
import org.silkframework.runtime.serialization._
import org.silkframework.util.{DPair, Uri}
import org.silkframework.workspace.annotation.UiAnnotations

import scala.xml.Node

/**
 * A linkage rule specifies the conditions which must hold true so that a link is generated between two entities.
 */
case class LinkageRule(operator: Option[SimilarityOperator] = None,
                       filter: LinkFilter = LinkFilter(),
                       linkType: Uri = Uri.fromString("http://www.w3.org/2002/07/owl#sameAs"),
                       layout: RuleLayout = RuleLayout(),
                       uiAnnotations: UiAnnotations = UiAnnotations()
                      ) extends PluginObjectParameterNoSchema {

  // Make sure that all operators use unique identifiers
  operator.foreach(_.validateIds())

  /**
   * Computes the similarity between two entities.
   *
   * @param entities The entities to be compared.
   * @param limit If the confidence is below this limit, it will be capped to -1.0.
    * @return The confidence as a value between -1.0 and 1.0.
   *         -1.0 for definitive non-matches.
   *         +1.0 for definitive matches.
   */
  def apply(entities: DPair[Entity], limit: Double = 0.0): Double = {
    operator match {
      case Some(op) => op(entities, limit).getOrElse(-1.0)
      case None => -1.0
    }
  }

  /**
   * Indexes an entity.
   *
   * @param entity The entity to be indexed
   * @param limit The confidence limit
    * @return A set of (multidimensional) indexes. Entities within the threshold will always get the same index.
   */
  def index(entity: Entity, sourceOrTarget: Boolean, limit: Double = 0.0): Index = {
    operator match {
      case Some(op) => op.index(entity, sourceOrTarget, limit)
      case None => Index.empty
    }
  }
}

/**
 * Creates new linkage rules.
 */
object LinkageRule {
  /**
   * Creates a new linkage rule with one root operator.
   */
  def apply(operator: SimilarityOperator): LinkageRule = LinkageRule(Some(operator))

  /**
   * XML serialization format.
   */
  implicit object LinkageRuleFormat extends XmlFormat[LinkageRule] {

    import XmlSerialization._

    private val schemaLocation = "org/silkframework/LinkSpecificationLanguage.xsd"

    def read(node: Node)(implicit readContext: ReadContext): LinkageRule = {
      // Validate against XSD Schema
      ValidatingXMLReader.validate(node, schemaLocation)

      val link = (node \ "@linkType").text.trim
      LinkageRule(
        operator = (node \ "_").find(child => !Set("Filter", "RuleLayout", "UiAnnotations").contains(child.label)).map(fromXml[SimilarityOperator]),
        filter = (node \ "Filter").headOption.map(LinkFilter.fromXML).getOrElse(LinkFilter()),
        linkType = if(link.isEmpty) "http://www.w3.org/2002/07/owl#sameAs" else Uri.parse(link, readContext.prefixes),
        layout = (node \ "RuleLayout").headOption.map(rl => XmlSerialization.fromXml[RuleLayout](rl)).getOrElse(RuleLayout()),
        uiAnnotations = (node \ "UiAnnotations").headOption.map(uiAnnotations => XmlSerialization.fromXml[UiAnnotations](uiAnnotations)).getOrElse(UiAnnotations())
      )
    }

    def write(value: LinkageRule)(implicit writeContext: WriteContext[Node]): Node = {
      <LinkageRule linkType={value.linkType.serialize(writeContext.prefixes)}>
        {value.operator.toList.map(toXml[SimilarityOperator])}
        {value.filter.toXML}
        {XmlSerialization.toXml(value.layout)}
        {XmlSerialization.toXml(value.uiAnnotations)}
      </LinkageRule>
    }
  }
}