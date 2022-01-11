import { RuleEditorModel } from "./RuleEditorModel";
import React from "react";
import { RuleEditorView } from "./RuleEditorView";
import { createRuleEditorContext } from "./contexts/RuleEditorContext";
import { IViewActions } from "../../plugins/PluginRegistry";

export interface RuleEditorProps<TASK_TYPE> {
    /** Project ID the task is in. */
    projectId: string;
    /** The task the rules are being edited of. */
    taskId: string;
    /** Function to fetch the actual task data to initialize the editor. */
    fetchTaskData: (projectId: string, taskId: string) => Promise<TASK_TYPE | undefined> | TASK_TYPE | undefined;
    /** Generic actions and callbacks on views. */
    viewActions?: IViewActions;
}

/**
 * Generic rule editor that can be used to build tree-line rule operator graphs.
 */
export const RuleEditor = <TASK_TYPE extends object>({
    projectId,
    taskId,
    fetchTaskData,
}: RuleEditorProps<TASK_TYPE>) => {
    const [taskData, setTaskData] = React.useState<TASK_TYPE | undefined>(undefined);
    // Fetch the task data
    React.useEffect(() => {
        fetchData();
    }, [projectId, taskId]);
    const fetchData = async () => {
        setTaskData(await fetchTaskData(projectId, taskId));
    };
    const RuleEditorContext = createRuleEditorContext<TASK_TYPE, any>();
    return (
        <RuleEditorContext.Provider
            value={{
                editedItem: taskData,
                operatorList: [], // TODO
            }}
        >
            <RuleEditorModel>
                <RuleEditorView />
            </RuleEditorModel>
        </RuleEditorContext.Provider>
    );
};
