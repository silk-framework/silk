import { combineReducers } from "@reduxjs/toolkit";
import { mainEditorSlice } from "./mainEditorSlice";
import { sidebarSlice } from "./sidebarSlice";

export { default as flowEditorSel } from "./selectors";

export default combineReducers({
    sidebar: sidebarSlice.reducer,
    mainEditor: mainEditorSlice.reducer,
});
