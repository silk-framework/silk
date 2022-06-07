import { RuleEditorModel } from "./model/RuleEditorModel";
import React from "react";
import { RuleEditorView } from "./view/RuleEditorView";
import { RuleEditorContext } from "./contexts/RuleEditorContext";
import { IViewActions } from "../../plugins/PluginRegistry";
import {
    IParameterSpecification,
    IRuleOperator,
    IRuleOperatorNode,
    IRuleSideBarFilterTabConfig,
    IRuleSidebarPreConfiguredOperatorsTabConfig,
    RuleEditorValidationNode,
    RuleOperatorPluginType,
    RuleSaveResult,
} from "./RuleEditor.typings";
import ErrorBoundary from "../../../ErrorBoundary";
import { ReactFlowProvider } from "react-flow-renderer";
import utils from "./RuleEditor.utils";

export type RuleOperatorFetchFnType = (
    pluginId: string,
    pluginType?: RuleOperatorPluginType
) => IRuleOperator | undefined;

export interface RuleEditorProps<RULE_TYPE, OPERATOR_TYPE> {
    /** Optional title that is shown above the toolbar. */
    editorTitle?: string;
    /** Project ID the task is in. */
    projectId: string;
    /** The task the rules are being edited of. */
    taskId: string;
    /** Function to fetch the actual task data to initialize the editor. */
    fetchRuleData: (projectId: string, taskId: string) => Promise<RULE_TYPE | undefined> | RULE_TYPE | undefined;
    /** Save rule. If true is returned saving was successful, else it failed. */
    saveRule: (
        ruleOperatorNodes: IRuleOperatorNode[],
        originalRuleData: RULE_TYPE
    ) => Promise<RuleSaveResult> | RuleSaveResult;
    /** Fetch available rule operators. */
    fetchRuleOperators: () => Promise<OPERATOR_TYPE[] | undefined> | OPERATOR_TYPE[] | undefined;
    /** Converts the custom format to the internal rule operator format. */
    convertRuleOperator: (
        op: OPERATOR_TYPE,
        addAdditionParameterSpecifications: (
            pluginDetails: OPERATOR_TYPE
        ) => [id: string, spec: IParameterSpecification][]
    ) => IRuleOperator;
    /** Converts the external rule representation into the internal rule representation. */
    convertToRuleOperatorNodes: (ruleData: RULE_TYPE, ruleOperator: RuleOperatorFetchFnType) => IRuleOperatorNode[];
    /** Generic actions and callbacks on views. */
    viewActions?: IViewActions;
    /** Additional rule operator plugins that are not returned via the fetchRuleOperators method. */
    additionalRuleOperators?: IRuleOperator[];
    /** Function to add additional parameter (specifications) to a rule operator based on the original operator. */
    addAdditionParameterSpecifications?: (operator: OPERATOR_TYPE) => [id: string, spec: IParameterSpecification][];
    /** Specifies the allowed connections. Only connections that return true are allowed. */
    validateConnection: (
        fromRuleOperatorNode: RuleEditorValidationNode,
        toRuleOperatorNode: RuleEditorValidationNode,
        targetPortIdx: number
    ) => boolean;
    /** Tabs that allow to show different rule operators or only a subset. */
    tabs?: (IRuleSideBarFilterTabConfig | IRuleSidebarPreConfiguredOperatorsTabConfig)[];
    /** Additional components that will be placed in the tool bar left to the save button. */
    additionalToolBarComponents?: () => JSX.Element | JSX.Element[];
}

const READ_ONLY_QUERY_PARAMETER = "readOnly";
/**
 * Generic rule editor that can be used to build tree-line rule operator graphs.
 */
const RuleEditor = <TASK_TYPE extends object, OPERATOR_TYPE extends object>({
    projectId,
    taskId,
    fetchRuleData,
    fetchRuleOperators,
    convertRuleOperator,
    convertToRuleOperatorNodes,
    saveRule,
    additionalRuleOperators,
    addAdditionParameterSpecifications,
    validateConnection,
    tabs,
    viewActions,
    additionalToolBarComponents,
    editorTitle
}: RuleEditorProps<TASK_TYPE, OPERATOR_TYPE>) => {
    // The task that contains the rule, e.g. transform or linking task
    const [taskData, setTaskData] = React.useState<TASK_TYPE | undefined>(undefined);
    // True while the task data is loaded
    const [taskDataLoading, setTaskDataLoading] = React.useState<boolean>(false);
    // The available operators for building the rule
    const [operators, setOperators] = React.useState<OPERATOR_TYPE[]>([]);
    // True while operators are loaded
    const [operatorsLoading, setOperatorsLoading] = React.useState<boolean>(false);
    // The internal rule operator node model
    const [initialRuleOperatorNodes, setInitialRuleOperatorNodes] = React.useState<IRuleOperatorNode[] | undefined>(
        undefined
    );
    // The list of available operators that can be added to the canvas
    const [operatorList, setOperatorList] = React.useState<IRuleOperator[] | undefined>(undefined);
    /* A map that connects pluginId to all operators with that ID. In theory there could be plugins with the same ID in different plugin types,
       so we need to have an array. */
    const [operatorMap, setOperatorMap] = React.useState<Map<string, IRuleOperator[]> | undefined>(undefined);
    const [operatorSpec, setOperatorSpec] = React.useState<
        Map<string, Map<string, IParameterSpecification>> | undefined
    >(undefined);
    const readOnlyMode =
        (new URLSearchParams(window.location.search).get(READ_ONLY_QUERY_PARAMETER) ?? "").toLowerCase() === "true";
    const [lastSaveResult, setLastSaveResult] = React.useState<RuleSaveResult | undefined>(undefined);

    /** This should be used instead of calling setLastSaveResult directly. */
    const updateLastSaveResult = (saveResult: RuleSaveResult | undefined) => {
        // This makes sure that the notifications are shown again
        setLastSaveResult(undefined);
        if (saveResult !== undefined) {
            setLastSaveResult(saveResult);
        }
    };

    // Fetch the task data
    React.useEffect(() => {
        fetchData();
    }, [projectId, taskId]);

    // Convert task data to internal model
    React.useEffect(() => {
        if (taskData && operatorMap) {
            const getOperatorNode = (pluginId: string, pluginType?: string) => {
                const operatorPlugins = operatorMap.get(pluginId);
                if (!operatorPlugins) {
                    console.warn("No plugin operator with ID " + pluginId + " found!");
                } else {
                    return pluginType
                        ? operatorPlugins.find((plugin) => plugin.pluginType === pluginType)
                        : operatorPlugins[0];
                }
            };
            const nodes = convertToRuleOperatorNodes(taskData, getOperatorNode);
            setInitialRuleOperatorNodes(nodes);
        }
    }, [taskData, operatorMap]);

    // Convert available operators
    React.useEffect(() => {
        if (operators.length > 0) {
            const ops: IRuleOperator[] = [];
            (additionalRuleOperators ?? []).forEach((additionalOp) => {
                ops.push(additionalOp);
            });
            const addAdditionalParams = addAdditionParameterSpecifications ?? (() => []);
            operators.forEach((op) => ops.push(convertRuleOperator(op, addAdditionalParams)));
            const operatorSpec = new Map(
                ops.map((op) => [op.pluginId, new Map(Object.entries(op.parameterSpecification))])
            );

            const operatorMap = new Map<string, IRuleOperator[]>();
            ops.forEach((op) => operatorMap.set(op.pluginId, []));
            ops.forEach((op) => {
                operatorMap.get(op.pluginId)!!.push(op);
            });
            setOperatorSpec(operatorSpec);
            setOperatorList(ops);
            setOperatorMap(operatorMap);
        }
    }, [operators]);

    const fetchData = async () => {
        setTaskDataLoading(true);
        try {
            setTaskData(await fetchRuleData(projectId, taskId));
        } finally {
            setTaskDataLoading(false);
        }
    };

    const saveRuleOperatorNodes = async (ruleNodeOperators: IRuleOperatorNode[]): Promise<RuleSaveResult> => {
        if (taskData) {
            const result = await saveRule(ruleNodeOperators, taskData);
            updateLastSaveResult(result);
            return result;
        } else {
            const error = {
                success: false,
                errorMessage: "No task data loaded, cannot save!",
                nodeErrors: [],
            };
            updateLastSaveResult(error);
            // unlikely to ever happen
            return error;
        }
    };

    // Fetch the operators
    React.useEffect(() => {
        fetchOperators();
    }, [projectId, taskId]);

    const fetchOperators = async () => {
        setOperatorsLoading(true);
        try {
            setOperators((await fetchRuleOperators()) ?? []);
        } finally {
            setOperatorsLoading(false);
        }
    };

    return (
        <RuleEditorContext.Provider
            value={{
                projectId,
                editedItemId: taskId,
                editedItem: taskData,
                operatorList,
                editedItemLoading: taskDataLoading,
                operatorListLoading: operatorsLoading,
                initialRuleOperatorNodes,
                saveRule: saveRuleOperatorNodes,
                convertRuleOperatorToRuleNode: utils.defaults.convertRuleOperatorToRuleNode,
                operatorSpec,
                validateConnection,
                tabs,
                viewActions,
                readOnlyMode,
                additionalToolBarComponents,
                lastSaveResult: lastSaveResult,
                editorTitle
            }}
        >
            <RuleEditorModel>
                <RuleEditorView />
            </RuleEditorModel>
        </RuleEditorContext.Provider>
    );
};

const WrappedRuleEditor = <RULE_TYPE extends object, OPERATOR_TYPE extends object>(
    props: RuleEditorProps<RULE_TYPE, OPERATOR_TYPE>
) => (
    <ErrorBoundary>
        <ReactFlowProvider>
            <RuleEditor<RULE_TYPE, OPERATOR_TYPE> {...props} />
        </ReactFlowProvider>
    </ErrorBoundary>
);

export default WrappedRuleEditor;
