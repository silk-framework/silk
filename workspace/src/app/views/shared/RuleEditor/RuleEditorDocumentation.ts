import { PluginDocumentation, RelatedPluginDocumentation } from "../modals/PluginDocumentation";
import { IRuleOperator, IRuleOperatorNode } from "./RuleEditor.typings";

type DocumentedRuleOperator = Pick<
    IRuleOperator | IRuleOperatorNode,
    "pluginId" | "label" | "description" | "markdownDocumentation" | "relatedPlugins"
>;
type RelatedRuleOperator = Pick<
    IRuleOperator,
    "pluginId" | "label" | "description" | "markdownDocumentation" | "relatedPlugins"
>;

/** Converts rule-operator data into the common plugin documentation model. */
export const ruleOperatorToPluginDocumentation = (
    ruleOperator: DocumentedRuleOperator,
    allRuleOperators: RelatedRuleOperator[] = [],
): PluginDocumentation => {
    const availableRuleOperator = allRuleOperators.find((operator) => operator.pluginId === ruleOperator.pluginId);
    const relatedPlugins = availableRuleOperator?.relatedPlugins ?? ruleOperator.relatedPlugins;

    return {
        key: ruleOperator.pluginId,
        title: ruleOperator.label,
        description: ruleOperator.description ?? availableRuleOperator?.description,
        markdownDocumentation: ruleOperator.markdownDocumentation ?? availableRuleOperator?.markdownDocumentation,
        relatedPlugins: relatedPlugins?.map((relatedPlugin): RelatedPluginDocumentation => {
            const relatedRuleOperator = allRuleOperators.find((operator) => operator.pluginId === relatedPlugin.id);

            return {
                plugin: {
                    key: relatedPlugin.id,
                    title: relatedRuleOperator?.label ?? relatedPlugin.id,
                    description: relatedRuleOperator?.description,
                    markdownDocumentation: relatedRuleOperator?.markdownDocumentation,
                },
                description: relatedPlugin.description || relatedRuleOperator?.description || "",
            };
        }),
    };
};
