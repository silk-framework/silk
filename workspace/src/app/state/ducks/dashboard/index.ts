import { combineReducers } from "redux";
import { filtersSlice } from "./filtersSlice";
import { previewSlice } from "./previewSlice";

export { default as dashboardOp } from './operations';
export { default as dashboardSel } from './selectors';

export default combineReducers({
    filters: filtersSlice.reducer,
    preview: previewSlice.reducer,
});
