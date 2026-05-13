import { IPreConfiguredRuleOperator } from "views/shared/RuleEditor/view/sidebar/RuleEditorOperatorSidebar.typings";
import {
    IRuleSideBarFilterTabConfig,
    IRuleOperator,
    IRuleSidebarPreConfiguredOperatorsTabConfig,
} from "../../../views/shared/RuleEditor/RuleEditor.typings";
import { EvaluationResultType } from "../linking/evaluation/LinkingRuleEvaluation";
import { PathWithMetaData } from "../shared/rules/rule.typings";
import { autoCompleteTransformSourcePath } from "./transform.requests";
import { EvaluatedTransformEntity } from "./transform.types";
import { SampleError } from "../../shared/SampleError/SampleError";
import { TaskContext } from "../../shared/projectTaskTabView/projectTaskTabView.typing";
import { IRuleBlockPort, IRuleBlockTaskParameters } from "../ruleBlock/ruleBlock.types";
import { IProjectTask } from "@ducks/shared/typings";
import { IPluginDetails } from "@ducks/common/typings";
import i18next from "i18next";

export interface IRuleBlockOperatorDetails {
    pluginType: "RuleBlock";
    pluginId: string;
    label: string;
    description?: string;
    ports: IRuleBlockPort[];
}

export type TransformRuleEditorOperator = IPluginDetails | IRuleBlockOperatorDetails;

const sortAlphabetically = (ruleOpA: IRuleOperator, ruleOpB: IRuleOperator) =>
    ruleOpA.label.toLowerCase() < ruleOpB.label.toLowerCase() ? -1 : 1;

const sortRuleBlockPorts = (ports: IRuleBlockPort[]): IRuleBlockPort[] =>
    [...ports].sort((left, right) => left.displayOrder - right.displayOrder || left.id.localeCompare(right.id));

const isRuleBlockOperator = (
    operator: TransformRuleEditorOperator,
): operator is IRuleBlockOperatorDetails => operator.pluginType === "RuleBlock";

const ruleBlockTaskToOperator = (
    ruleBlockTask: IProjectTask<IRuleBlockTaskParameters>,
): IRuleBlockOperatorDetails => ({
    pluginType: "RuleBlock",
    pluginId: ruleBlockTask.id,
    label: ruleBlockTask.metadata.label ?? ruleBlockTask.id,
    description: ruleBlockTask.metadata.description,
    ports: sortRuleBlockPorts(ruleBlockTask.data.parameters.ruleBlockModel?.ports ?? []),
});

const convertRuleBlockOperator = (ruleBlock: IRuleBlockOperatorDetails): IRuleOperator => ({
    pluginType: "RuleBlock",
    pluginId: ruleBlock.pluginId,
    label: ruleBlock.label,
    icon: "artefact-ruleblock",
    description: ruleBlock.description,
    categories: [i18next.t("common.dataTypes.ruleBlock")],
    parameterSpecification: {},
    portSpecification: {
        type: "named",
        inputPorts: ruleBlock.ports.map((port) => ({ id: port.id })),
    },
    tags: [],
    inputsCanBeSwitched: false,
});

const ruleBlockTab = (): IRuleSideBarFilterTabConfig => ({
    id: "ruleBlock",
    icon: "artefact-ruleblock",
    label: i18next.t("common.dataTypes.ruleBlock"),
    filterAndSort: (ops) => ops.filter((op) => op.pluginType === "RuleBlock").sort(sortAlphabetically),
    showOperatorsFromPreConfiguredOperatorTabsForQuery: false,
});

export const inputPathTab = (
    projectId: string,
    transformTaskId: string,
    ruleId: string,
    baseOperator: IRuleOperator,
    errorHandler: (err) => any,
    taskContext?: TaskContext
): IRuleSidebarPreConfiguredOperatorsTabConfig => {
    const category = "Source path";
    const inputPathTabConfig: IRuleSidebarPreConfiguredOperatorsTabConfig<PathWithMetaData> = {
        id: `sourcePaths`,
        icon: "data-sourcepath",
        label: "Source paths",
        defaultOperators: [
            {
                value: "",
                valueType: "",
                label: category,
            },
        ],
        fetchOperators: async (langPref: string) => {
            try {
                return (
                    await autoCompleteTransformSourcePath(projectId, transformTaskId, ruleId, "", taskContext)
                ).data.map((d) => ({
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
                categories: [category],
                parameterOverwrites: {
                    path: path.label ? { value: path.value, label: path.label } : path.value,
                },
                tags: path.valueType ? [path.valueType] : [],
                inputsCanBeSwitched: false,
            };
        },
        isOriginalOperator: (listItem) => (listItem as PathWithMetaData).valueType != null,
        itemSearchText: (listItem: PathWithMetaData, mergedWithOtherOperators: boolean) =>
            `${listItem.label ?? ""} ${listItem.value} ${listItem.valueType} ${
                mergedWithOtherOperators ? category : ""
            }`.toLowerCase(),
        itemLabel: (listItem: PathWithMetaData) => listItem.label ?? listItem.value,
        itemId: (listItem: PathWithMetaData) => `input path: ${listItem.value}`,
    };
    return inputPathTabConfig;
};

export const transformToValueMap = (transform: EvaluatedTransformEntity): Map<string, EvaluationResultType[number]> => {
    const valueMap = new Map<string, { error?: SampleError | null; value: string[] }>();

    const traverseTransformTree = (transform: EvaluatedTransformEntity) => {
        let error: SampleError | undefined = undefined;
        if (transform.error) {
            error = {
                error: transform.error,
                entity: "",
                stacktrace: transform.stacktrace,
                values: transform.children?.map((child) => child.values),
            };
        }
        valueMap.set(transform.operatorId, { value: transform.values, error: error });
        transform.children && transform.children.forEach((t) => traverseTransformTree(t));
    };

    traverseTransformTree(transform);
    return valueMap;
};

const transformEditorUtils = {
    convertRuleBlockOperator,
    inputPathTab,
    isRuleBlockOperator,
    ruleBlockTab,
    ruleBlockTaskToOperator,
    transformToValueMap,
};

export default transformEditorUtils;
