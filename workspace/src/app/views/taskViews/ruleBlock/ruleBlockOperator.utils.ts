import {
    IRuleOperator,
    IRuleSideBarFilterTabConfig,
} from "../../../views/shared/RuleEditor/RuleEditor.typings";
import { IRuleBlockPort, IRuleBlockSummary } from "./ruleBlock.types";
import i18next from "i18next";

export interface IRuleBlockOperatorDetails {
    pluginType: "RuleBlock";
    pluginId: string;
    label: string;
    description?: string;
    ports: IRuleBlockPort[];
}

const sortAlphabetically = (ruleOpA: IRuleOperator, ruleOpB: IRuleOperator) =>
    ruleOpA.label.toLowerCase() < ruleOpB.label.toLowerCase() ? -1 : 1;

const sortRuleBlockPorts = (ports: IRuleBlockPort[]): IRuleBlockPort[] =>
    [...ports].sort((left, right) => left.displayOrder - right.displayOrder || left.id.localeCompare(right.id));

const isRuleBlockOperator = (
    operator: { pluginType?: string },
): operator is IRuleBlockOperatorDetails => operator.pluginType === "RuleBlock";

const ruleBlockSummaryToOperator = (
    ruleBlockSummary: IRuleBlockSummary,
): IRuleBlockOperatorDetails => ({
    pluginType: "RuleBlock",
    pluginId: ruleBlockSummary.id,
    label: ruleBlockSummary.label,
    description: ruleBlockSummary.description,
    ports: sortRuleBlockPorts(ruleBlockSummary.ports),
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

const ruleBlockOperatorUtils = {
    convertRuleBlockOperator,
    isRuleBlockOperator,
    ruleBlockSummaryToOperator,
    ruleBlockTab,
};

export default ruleBlockOperatorUtils;
