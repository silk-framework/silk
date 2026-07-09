import type { TaskPlugin } from "@ducks/shared/typings";
import type { IProjectTask } from "@ducks/shared/typings";
import type {
    IComparisonOperator,
    ILinkingRule,
    ILinkingTaskParameters,
    ISimilarityOperator,
} from "../../../linking/linking.types";
import type {
    IRuleBlockInputExample,
    RuleBlockSnapshot,
    RuleBlockPort,
    IRuleBlockTaskParameters,
} from "../../../ruleBlock/ruleBlock.types";
import type { IComplexMappingRule } from "../../../transform/transform.types";
import type {
    IInputPortInput,
    IPathInput,
    IRuleBlockInput,
    ITransformOperator,
    RuleLayout,
} from "../rule.typings";
import type { IRuleOperatorNode } from "../../../../shared/RuleEditor/RuleEditor.typings";

const defaultLayout = (): RuleLayout => ({
    nodePositions: {},
});

const defaultUiAnnotations = () => ({
    stickyNotes: [],
});

const createRuleBlockPort = (overrides: Partial<RuleBlockPort> = {}): RuleBlockPort => ({
    id: "inputPortA",
    label: "Input A",
    description: "",
    displayOrder: 1,
    deprecated: false,
    ...overrides,
});

const createRuleBlockInputExample = (
    overrides: Partial<IRuleBlockInputExample> = {},
): IRuleBlockInputExample => ({
    id: "example-1",
    inputs: {
        inputPortA: ["Original value"],
    },
    ...overrides,
});

const createPathInput = (overrides: Partial<IPathInput> = {}): IPathInput => ({
    type: "pathInput",
    id: "pathInput",
    path: "",
    ...overrides,
});

const createInputPortInput = (overrides: Partial<IInputPortInput> = {}): IInputPortInput => ({
    type: "inputPortInput",
    id: "inputPortNode",
    portId: "inputPortA",
    ...overrides,
});

const createTransformInput = (overrides: Partial<ITransformOperator> = {}): ITransformOperator => ({
    type: "transformInput",
    id: "lowerCaseNode",
    function: "lowerCase",
    parameters: {},
    inputs: [createInputPortInput()],
    ...overrides,
});

const createRuleBlockInput = (overrides: Partial<IRuleBlockInput> = {}): IRuleBlockInput => ({
    type: "ruleBlockInput",
    id: "ruleBlockUsage",
    ruleBlockId: "normalizeName",
    bindings: [
        {
            portId: "inputPortA",
            input: createPathInput({
                id: "pathInput",
            }),
        },
    ],
    ...overrides,
});

const createRuleBlockTask = (
    ports: RuleBlockPort[] = [],
    inputExamples: IRuleBlockInputExample[] = [],
): IProjectTask<IRuleBlockTaskParameters> =>
    ({
        metadata: {
            label: "Rule block task",
        },
        taskType: "RuleBlock",
        id: "ruleBlockTask",
        project: "project1",
        data: {
            type: "RuleBlock",
            parameters: {
                ruleBlockModel: {
                    ports,
                    inputExamples,
                    layout: defaultLayout(),
                    uiAnnotations: defaultUiAnnotations(),
                },
            },
        },
    }) as IProjectTask<IRuleBlockTaskParameters>;

const createRuleBlockInspectionSnapshot = (
    overrides: Partial<RuleBlockSnapshot> = {},
): RuleBlockSnapshot => ({
    ports: overrides.ports ?? [],
    operatorTree: overrides.operatorTree,
    layout: overrides.layout ?? defaultLayout(),
    uiAnnotations: overrides.uiAnnotations ?? defaultUiAnnotations(),
});

const createInputPortNode = (overrides: Partial<IRuleOperatorNode> = {}): IRuleOperatorNode => ({
    nodeId: "inputPortNode",
    pluginType: "InputPortOperator",
    pluginId: "inputPort",
    label: "Input port",
    parameters: {
        portId: "inputPortA",
    },
    inputs: [],
    portSpecification: {
        type: "count",
        minInputPorts: 0,
        maxInputPorts: 0,
    },
    inputsCanBeSwitched: false,
    ...overrides,
});

const createRuleOperatorNode = (overrides: Partial<IRuleOperatorNode> & { nodeId?: string } = {}): IRuleOperatorNode => {
    const nodeId = overrides.nodeId ?? "nodeId";
    const tags = Object.prototype.hasOwnProperty.call(overrides, "tags") ? overrides.tags : [];
    return {
        nodeId,
        label: overrides.label ?? nodeId,
        pluginType: overrides.pluginType ?? "unknown",
        pluginId: overrides.pluginId ?? "unknown",
        parameters: overrides.parameters ?? {},
        portSpecification: overrides.portSpecification ?? {
            type: "count",
            minInputPorts: 0,
        },
        tags,
        inputsCanBeSwitched: overrides.inputsCanBeSwitched ?? false,
        inputs: overrides.inputs ?? [],
        ...overrides,
    };
};

const createTransformNode = (overrides: Partial<IRuleOperatorNode> = {}): IRuleOperatorNode => ({
    nodeId: "lowerCaseNode",
    pluginType: "TransformOperator",
    pluginId: "lowerCase",
    label: "Lower case",
    parameters: {},
    inputs: ["inputPortNode"],
    portSpecification: {
        type: "count",
        minInputPorts: 1,
    },
    inputsCanBeSwitched: false,
    ...overrides,
});

const createRuleBlockUsageNode = (overrides: Partial<IRuleOperatorNode> = {}): IRuleOperatorNode => ({
    nodeId: "ruleBlockUsage",
    pluginType: "RuleBlock",
    pluginId: "normalizeName",
    label: "Normalize Name",
    parameters: {},
    inputs: ["pathInput"],
    portSpecification: {
        type: "count",
        minInputPorts: 1,
        maxInputPorts: 1,
    },
    inputsCanBeSwitched: false,
    ...overrides,
});

const createComparisonNode = (overrides: Partial<IRuleOperatorNode> = {}): IRuleOperatorNode => ({
    nodeId: "compareLabels",
    pluginType: "ComparisonOperator",
    pluginId: "equality",
    label: "Equality",
    parameters: {
        threshold: "0.0",
        weight: "1",
    },
    inputs: ["sourceRuleBlockUsage", "targetLabel"],
    portSpecification: {
        type: "count",
        minInputPorts: 2,
        maxInputPorts: 2,
    },
    inputsCanBeSwitched: false,
    ...overrides,
});

const createComplexMappingRule = (overrides: Partial<IComplexMappingRule> = {}): IComplexMappingRule => ({
    id: "mappingRule",
    type: "complex",
    metadata: {
        label: "",
        description: "",
    },
    sourcePaths: [],
    operator: createPathInput({
        id: "originalPath",
        path: "name",
    }),
    layout: defaultLayout(),
    uiAnnotations: defaultUiAnnotations(),
    ...overrides,
});

const createComparisonOperator = (overrides: Partial<IComparisonOperator> = {}): IComparisonOperator => ({
    id: "comparison-node",
    type: "Comparison",
    weight: 1,
    metric: "equality",
    threshold: 0,
    parameters: {},
    sourceInput: createPathInput({
        id: "source-path",
        path: "name",
    }),
    targetInput: createPathInput({
        id: "target-path",
        path: "targetName",
    }),
    ...overrides,
});

const createAggregationOperator = (
    overrides: Partial<ISimilarityOperator> & { inputs?: ISimilarityOperator[] } = {},
): ISimilarityOperator => ({
    id: "aggregation-node",
    type: "Aggregation",
    weight: 1,
    aggregator: "max",
    parameters: {},
    inputs: overrides.inputs ?? [createComparisonOperator()],
    ...overrides,
}) as ISimilarityOperator;

const createLinkingRule = (overrides: Partial<ILinkingRule> = {}): ILinkingRule => ({
    filter: {},
    linkType: "",
    excludeSelfReferences: false,
    layout: defaultLayout(),
    uiAnnotations: defaultUiAnnotations(),
    operator: undefined,
    ...overrides,
});

const createLinkingTask = (
    overrides: Partial<TaskPlugin<ILinkingTaskParameters>> = {},
): TaskPlugin<ILinkingTaskParameters> =>
    ({
        id: "linkingTask",
        project: "project1",
        label: "Linking task",
        type: "task",
        pluginType: "linking",
        parameters: {
            source: undefined,
            target: undefined,
            rule: createLinkingRule(),
            output: undefined,
            referenceLinks: undefined,
            linkLimit: undefined,
            matchingExecutionTimeout: undefined,
        },
        ...overrides,
    } as unknown as TaskPlugin<ILinkingTaskParameters>);

const ruleTestHelper = {
    createAggregationOperator,
    createComparisonNode,
    createComparisonOperator,
    createComplexMappingRule,
    createInputPortInput,
    createInputPortNode,
    createLinkingRule,
    createLinkingTask,
    createPathInput,
    createRuleBlockInput,
    createRuleBlockInputExample,
    createRuleBlockInspectionSnapshot,
    createRuleOperatorNode,
    createRuleBlockPort,
    createRuleBlockTask,
    createRuleBlockUsageNode,
    createTransformInput,
    createTransformNode,
    defaultLayout,
    defaultUiAnnotations,
};

export default ruleTestHelper;
