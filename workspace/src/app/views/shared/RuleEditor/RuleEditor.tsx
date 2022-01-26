import { RuleEditorModel } from "./RuleEditorModel";
import React from "react";
import { RuleEditorView } from "./RuleEditorView";
import { createRuleEditorContext } from "./contexts/RuleEditorContext";
import { IViewActions } from "../../plugins/PluginRegistry";
import { IRuleOperator, IRuleOperatorNode } from "./RuleEditor.typings";

export interface RuleEditorProps<RULE_TYPE, OPERATOR_TYPE> {
    /** Project ID the task is in. */
    projectId: string;
    /** The task the rules are being edited of. */
    taskId: string;
    /** Function to fetch the actual task data to initialize the editor. */
    fetchRuleData: (projectId: string, taskId: string) => Promise<RULE_TYPE | undefined> | RULE_TYPE | undefined;
    /** Save rule. If true is returned saving was successful, else it failed. TODO: Missing rule tree structure */
    saveRule: (ruleTree, originalRuleData: RULE_TYPE) => Promise<boolean> | boolean;
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
export const RuleEditor = <TASK_TYPE extends object, OPERATOR_TYPE extends object>({
    projectId,
    taskId,
    fetchRuleData,
    fetchRuleOperators,
}: RuleEditorProps<TASK_TYPE, OPERATOR_TYPE>) => {
    // The task that contains the rule, e.g. transform or linking task
    const [taskData, setTaskData] = React.useState<TASK_TYPE | undefined>(undefined);
    // True while the task data is loaded
    const [taskDataLoading, setTaskDataLoading] = React.useState<boolean>(false);
    // The available operators for building the rule
    const [operators, setOperators] = React.useState<OPERATOR_TYPE[]>([]);
    // True while operators are loaded
    const [operatorsLoading, setOperatorsLoading] = React.useState<boolean>(false);

    // Fetch the task data
    React.useEffect(() => {
        fetchData();
    }, [projectId, taskId]);

    const fetchData = async () => {
        setTaskDataLoading(true);
        try {
            setTaskData(await fetchRuleData(projectId, taskId));
        } finally {
            setTaskDataLoading(false);
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

    const RuleEditorContext = createRuleEditorContext<TASK_TYPE, OPERATOR_TYPE>();

    return (
        <RuleEditorContext.Provider
            value={{
                editedItem: taskData,
                operatorList: operators,
                editedItemLoading: taskDataLoading,
                operatorListLoading: operatorsLoading,
            }}
        >
            <RuleEditorModel>
                <RuleEditorView />
            </RuleEditorModel>
        </RuleEditorContext.Provider>
    );
};
