import { createSelector } from "@reduxjs/toolkit";
import { IStore } from "store/typings/IStore";
import { ISideBarState } from "./typings";

const sidebarSelector = (state: IStore): ISideBarState => state.flowEditor.sidebar;

const requestLoadingSelector = createSelector([sidebarSelector], (sidebar) => sidebar.loading);

const tasksListSelector = createSelector(sidebarSelector, (sidebar) => sidebar.results);

export default {
    requestLoadingSelector,
    tasksListSelector,
};
