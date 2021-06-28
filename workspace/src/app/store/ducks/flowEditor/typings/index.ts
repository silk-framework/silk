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
