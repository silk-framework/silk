import { combineReducers } from "@reduxjs/toolkit";
import { sidebarSlice } from "./sidebarSlice";

export { default as flowEditorSel } from "./selectors";

export default combineReducers({
    sidebar: sidebarSlice.reducer,
});
