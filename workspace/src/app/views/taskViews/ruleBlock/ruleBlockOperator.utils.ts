import {
    IRuleOperator,
    IRuleSideBarFilterTabConfig,
} from "../../../views/shared/RuleEditor/RuleEditor.typings";
import { RuleBlockPort, IRuleBlockSummary } from "./ruleBlock.types";
import i18next from "i18next";

export interface IRuleBlockOperatorDetails {
    pluginType: "RuleBlock";
    pluginId: string;
    label: string;
    description?: string;
    ports: RuleBlockPort[];
}

const RULE_BLOCK_TAG = "Rule block";

const sortAlphabetically = (ruleOpA: IRuleOperator, ruleOpB: IRuleOperator) =>
    ruleOpA.label.toLowerCase() < ruleOpB.label.toLowerCase() ? -1 : 1;

const sortRuleBlockPorts = (ports: RuleBlockPort[]): RuleBlockPort[] =>
    [...ports].sort((left, right) => left.displayOrder - right.displayOrder || left.id.localeCompare(right.id));

/** Checks whether a rule editor operator represents a reusable rule block. */
const isRuleBlockOperator = (
    operator: { pluginType?: string },
): operator is IRuleBlockOperatorDetails => operator.pluginType === "RuleBlock";

/** Converts a lightweight rule block summary into the sidebar/operator representation. */
const ruleBlockSummaryToOperator = (
    ruleBlockSummary: IRuleBlockSummary,
): IRuleBlockOperatorDetails => ({
    pluginType: "RuleBlock",
    pluginId: ruleBlockSummary.id,
    label: ruleBlockSummary.label,
    description: ruleBlockSummary.description,
    ports: sortRuleBlockPorts(ruleBlockSummary.ports),
});

/** Converts a reusable rule block summary into a named-port rule editor operator. */
const convertRuleBlockOperator = (ruleBlock: IRuleBlockOperatorDetails): IRuleOperator => ({
    pluginType: "RuleBlock",
    pluginId: ruleBlock.pluginId,
    label: ruleBlock.label,
    icon: "artefact-ruleblock",
    description: ruleBlock.description,
    categories: [],
    parameterSpecification: {},
    portSpecification: {
        type: "named",
        inputPorts: ruleBlock.ports.map((port) => ({ id: port.id })),
    },
    tags: [RULE_BLOCK_TAG],
    inputsCanBeSwitched: false,
});

/** Creates the dedicated sidebar tab configuration for reusable rule blocks. */
const ruleBlockTab = (): IRuleSideBarFilterTabConfig => ({
    id: "ruleBlock",
    icon: "artefact-ruleblock",
    label: i18next.t("common.dataTypes.ruleblock"),
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
