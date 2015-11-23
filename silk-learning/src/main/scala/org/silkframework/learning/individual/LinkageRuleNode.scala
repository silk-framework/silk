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

package org.silkframework.learning.individual

import org.silkframework.rule.LinkageRule

case class LinkageRuleNode(aggregation: Option[OperatorNode]) extends Node {
  override val children = aggregation.toList

  override def updateChildren(children: List[Node]) = {
    LinkageRuleNode(children.headOption.map(_.asInstanceOf[OperatorNode]))
  }

  def build = LinkageRule(aggregation.map(_.build))

  override def toString = build.toString
}

object LinkageRuleNode {
  def load(linkageRule: LinkageRule) = {
    LinkageRuleNode(linkageRule.operator.map(OperatorNode.load))
  }
}
