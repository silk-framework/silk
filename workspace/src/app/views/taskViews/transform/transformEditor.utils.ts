import { IPreConfiguredRuleOperator } from "views/shared/RuleEditor/view/sidebar/RuleEditorOperatorSidebar.typings";

import {
    IRuleOperator,
    IRuleSidebarPreConfiguredOperatorsTabConfig,
} from "../../../views/shared/RuleEditor/RuleEditor.typings";
import { EvaluationResultType } from "../linking/evaluation/LinkingRuleEvaluation";
import { PathWithMetaData } from "../shared/rules/rule.typings";
import { autoCompleteTransformSourcePath } from "./transform.requests";
import { EvaluatedTransformEntity } from "./transform.types";

export const inputPathTab = (
    projectId: string,
    transformTaskId: string,
    ruleId: string,
    baseOperator: IRuleOperator,
    errorHandler: (err) => any
): IRuleSidebarPreConfiguredOperatorsTabConfig => {
    const inputPathTabConfig: IRuleSidebarPreConfiguredOperatorsTabConfig<PathWithMetaData> = {
        id: `sourcePaths`,
        icon: "data-sourcepath",
        label: "Source paths",
        fetchOperators: async (langPref: string) => {
            try {
                return (await autoCompleteTransformSourcePath(projectId, transformTaskId, ruleId)).data.map((d) => ({
                    valueType: "",
                    ...d,
                })) as PathWithMetaData[];
            } catch (ex) {
                errorHandler(ex);
            }
        },
        convertToOperator: (path: PathWithMetaData): IPreConfiguredRuleOperator => {
            const { pluginId, pluginType, icon } = baseOperator;
            return {
                pluginId,
                pluginType,
                icon,
                label: path.label ?? path.value,
                description: path.label !== path.value ? path.value : undefined,
                categories: ["Source path"],
                parameterOverwrites: {
                    path: path.label ? { value: path.value, label: path.label } : path.value,
                },
                tags: path.valueType ? [path.valueType] : [],
                inputsCanBeSwitched: false,
            };
        },
        isOriginalOperator: (listItem) => (listItem as PathWithMetaData).valueType != null,
        itemSearchText: (listItem: PathWithMetaData) =>
            `${listItem.label ?? ""} ${listItem.value} ${listItem.valueType}`.toLowerCase(),
        itemLabel: (listItem: PathWithMetaData) => listItem.label ?? listItem.value,
        itemId: (listItem: PathWithMetaData) => listItem.value,
    };
    return inputPathTabConfig;
};

export const transformToValueMap = (transform: EvaluatedTransformEntity): Map<string, EvaluationResultType[number]> => {
    const valueMap = new Map<string, { error?: string | null; value: string[] }>();

    const traverseTransformTree = (transform: EvaluatedTransformEntity) => {
        valueMap.set(transform.operatorId, { value: transform.values, error: transform.error });
        transform.children && transform.children.forEach((t) => traverseTransformTree(t));
    };

    traverseTransformTree(transform);
    return valueMap;
};
