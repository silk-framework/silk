import { ISideBarState } from "./IEditorSidebar";

export * from "./IEditorSidebar";

/**** thunk types ****/
export enum TASK_SEARCH_IDS {
    searchList = "searchItems/label",
}

/**complete state for the editor */
export interface IEditorState {
    sidebar: ISideBarState;
}
