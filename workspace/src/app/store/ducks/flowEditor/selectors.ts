import { createSelector } from "@reduxjs/toolkit";
import { IStore } from "store/typings/IStore";
import { FlowEditorState } from "./typings";

/**** MainEditor ****/
const editorSel = (state: IStore): FlowEditorState => state.flowEditor.mainEditor;
const getPortsConfigSel = createSelector([editorSel], (editor) => editor.portsConfig);
const getPortsPendingState = createSelector([editorSel], (editor) => editor.portsConfigLoading);

export default {
    getPortsConfigSel,
    getPortsPendingState,
};
