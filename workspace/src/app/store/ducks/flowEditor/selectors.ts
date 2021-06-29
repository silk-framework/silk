import { createSelector } from "@reduxjs/toolkit";
import { IStore } from "store/typings/IStore";
import { FlowEditorState, ISideBarState } from "./typings";

/**** SideBar ****/
const sidebarSelector = (state: IStore): ISideBarState => state.flowEditor.sidebar;
const requestLoadingSelector = createSelector([sidebarSelector], (sidebar) => sidebar.loading);
const tasksListSelector = createSelector(sidebarSelector, (sidebar) => sidebar.results);

/**** MainEditor ****/
const editorSel = (state: IStore): FlowEditorState => state.flowEditor.mainEditor;
const getPortsConfigSel = createSelector([editorSel], (editor) => editor.portsConfig);
const getPortsPendingState = createSelector([editorSel], (editor) => editor.portsConfigLoading);

export default {
    requestLoadingSelector,
    tasksListSelector,
    getPortsConfigSel,
    getPortsPendingState,
};
