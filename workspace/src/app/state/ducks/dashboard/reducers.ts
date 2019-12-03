import { combineReducers } from "@reduxjs/toolkit";
import dashboardFiltersReducers from "./filters/reducers";
import dashboardPreviewReducers from "./preview/reducers";

const dashboard = combineReducers({
    filters: dashboardFiltersReducers,
    preview: dashboardPreviewReducers,
});

export default dashboard;
