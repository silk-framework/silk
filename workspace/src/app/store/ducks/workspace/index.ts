import { combineReducers } from "redux";
import { filtersSlice } from "./filtersSlice";
import { previewSlice } from "./previewSlice";

export { default as workspaceOp } from './operations';
export { default as workspaceSel } from './selectors';

export default combineReducers({
    filters: filtersSlice.reducer,
    preview: previewSlice.reducer,
});
