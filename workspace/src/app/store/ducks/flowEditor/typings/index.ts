import { ISideBarState } from "./IEditorSidebar";
import { FlowEditorState } from "./IMainEditor";

export * from "./IEditorSidebar";
export * from "./IMainEditor";

/**** thunk types ****/
export enum EDITOR_ASYNC_TYPES {
    searchList = "searchItems/label",
    portConfiguration = "get/portsConfig",
}

/*** complete state for the editor ***/
export interface IEditorState {
    sidebar: ISideBarState;
    mainEditor: FlowEditorState;
}

/**** Config types ****/
export type ports = {
    minPorts: number;
    maxPorts?: number;
};

type PortObjectType = {
    [key: string]: ports;
};

export interface PortConfigResponse {
    byItemType: PortObjectType;
    byNodeId: PortObjectType;
    byTaskId: PortObjectType;
}

interface IWorkflowNode {
    id: string;
    task: string;
    // Input workflow node IDs
    inputs: string[];
    // Output workflow node IDs
    outputs: string[];
    posX: number;
    posY: number;
    outputPriority?: number;
}

/** A dataset workflow node */
export interface IDatasetWorkflowNode extends IWorkflowNode {}

/** An operator workflow node */
export interface IOperatorWorkflowNode extends IWorkflowNode {
    // The workflow node IDs that are connected to the config input
    configInputs: string[];
    // The workflow nodes where the error output should be written
    errorOutput: string[];
}

/** Workflow task */
export interface IWorkflow {
    datasets: IDatasetWorkflowNode[];
    operators: IOperatorWorkflowNode[];
}
