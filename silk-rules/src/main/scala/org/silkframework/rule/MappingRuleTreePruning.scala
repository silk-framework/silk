package org.silkframework.rule

import org.silkframework.util.Identifier

/**
  * Prunes a transform rule tree down to a set of selected rules, keeping just enough structure
  * to produce meaningful RDF output for the selection.
  *
  * Semantics:
  *  - A selected node is kept as-is, including its whole subtree. Selecting an object mapping
  *    therefore yields its type triples, its URI (subject) and all of its child triples
  *    ("parent ⇒ all children").
  *  - An unselected container that has a selected descendant is kept as a structural shell:
  *    only its URI rule is retained (so the descendant triples have a subject IRI) together with
  *    the pruned children and any individually selected type rules. The shell's own (unselected)
  *    type rules and sibling value rules are dropped.
  *  - Everything else is dropped.
  */
object MappingRuleTreePruning {

  /**
    * Prunes the root mapping rule to the selected rule ids.
    *
    * @param root            The root mapping rule of a transform spec.
    * @param selectedRuleIds The ids of the rules (nodes) the caller selected.
    * @return The pruned root rule, or None if the selection does not produce any output.
    */
  def pruneRoot(root: RootMappingRule, selectedRuleIds: Set[Identifier]): Option[RootMappingRule] = {
    prune(root, selectedRuleIds) map {
      case prunedRoot: RootMappingRule => prunedRoot
      // withChildren preserves the concrete type, so this fallback should not be reached in practice.
      case other => root.copy(rules = other.rules)
    }
  }

  private def prune(rule: TransformRule, selected: Set[Identifier]): Option[TransformRule] = {
    if (selected.contains(rule.id)) {
      // Selected node: keep its entire subtree (type, URI/label and all child triples).
      Some(rule)
    } else {
      rule match {
        case container: ContainerTransformRule =>
          val prunedProperties = container.rules.propertyRules.flatMap(prune(_, selected))
          val selectedTypeRules = container.rules.typeRules.filter(t => selected.contains(t.id))
          if (prunedProperties.isEmpty && selectedTypeRules.isEmpty) {
            None
          } else {
            // Structural shell: retain the URI rule so the kept children have a subject IRI,
            // but drop this (unselected) node's own type rules.
            val shellChildren = container.rules.uriRule.toSeq ++ selectedTypeRules ++ prunedProperties
            Some(container.withChildren(shellChildren))
          }
        case _ =>
          // Unselected value/URI/type leaf.
          None
      }
    }
  }
}
