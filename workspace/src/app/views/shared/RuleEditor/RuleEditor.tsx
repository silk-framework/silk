import { RuleEditorModel } from "./model/RuleEditorModel";
import React from "react";
import { RuleEditorView } from "./RuleEditorView";
import { RuleEditorContext } from "./contexts/RuleEditorContext";
import { IViewActions } from "../../plugins/PluginRegistry";
import { IRuleOperator, IRuleOperatorNode } from "./RuleEditor.typings";
import ErrorBoundary from "../../../ErrorBoundary";
import { ReactFlowProvider } from "react-flow-renderer";
import utils from "./RuleEditor.utils";

export interface RuleEditorProps<RULE_TYPE, OPERATOR_TYPE> {
    /** Project ID the task is in. */
    projectId: string;
    /** The task the rules are being edited of. */
    taskId: string;
    /** Function to fetch the actual task data to initialize the editor. */
    fetchRuleData: (projectId: string, taskId: string) => Promise<RULE_TYPE | undefined> | RULE_TYPE | undefined;
    /** Save rule. If true is returned saving was successful, else it failed. */
    saveRule: (ruleOperatorNodes: IRuleOperatorNode[], originalRuleData: RULE_TYPE) => Promise<boolean> | boolean;
    /** Fetch available rule operators. */
    fetchRuleOperators: () => Promise<OPERATOR_TYPE[] | undefined> | OPERATOR_TYPE[] | undefined;
    /** Converts the custom format to the internal rule operator format. */
    convertRuleOperator: (op: OPERATOR_TYPE) => IRuleOperator;
    /** Converts the external rule representation into the internal rule representation. */
    convertToRuleOperatorNodes: (ruleData: RULE_TYPE) => IRuleOperatorNode[];
    /** Generic actions and callbacks on views. */
    viewActions?: IViewActions;
}

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

    // Fetch the task data
    React.useEffect(() => {
        fetchData();
    }, [projectId, taskId]);

    // Convert task data to internal model
    React.useEffect(() => {
        if (taskData) {
            const nodes = convertToRuleOperatorNodes(taskData);
            setInitialRuleOperatorNodes(nodes);
        }
    }, [taskData]);

    // Convert available operators
    React.useEffect(() => {
        if (operators) {
            const ops = operators.map((op) => convertRuleOperator(op));
            setOperatorList(ops);
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

    const saveRuleOperatorNodes = async (ruleNodeOperators: IRuleOperatorNode[]) => {
        if (taskData) {
            return saveRule(ruleNodeOperators, taskData);
        } else {
            console.error("No task data loaded, cannot save!");
            return false;
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
                editedItem: taskData,
                operatorList,
                editedItemLoading: taskDataLoading,
                operatorListLoading: operatorsLoading,
                initialRuleOperatorNodes,
                saveRule: saveRuleOperatorNodes,
                convertRuleOperatorToRuleNode: utils.defaults.convertRuleOperatorToRuleNode,
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
