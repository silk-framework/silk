/**** Config types ****/
export interface IPorts {
    minInputPorts: number;
    maxInputPorts?: number;
}

type PortObjectType = {
    [key: string]: IPorts;
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
