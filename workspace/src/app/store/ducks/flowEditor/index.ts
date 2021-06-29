import { combineReducers } from "@reduxjs/toolkit";
import { mainEditorSlice } from "./mainEditorSlice";

export { default as flowEditorSel } from "./selectors";

export default combineReducers({
    mainEditor: mainEditorSlice.reducer,
});
