import { FlowEditorState } from "./IMainEditor";

export * from "./IMainEditor";

/**** thunk types ****/
export enum EDITOR_ASYNC_TYPES {
    portConfiguration = "get/portsConfig",
}

/*** complete state for the editor ***/
export interface IEditorState {
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
