import { createSelector } from "@reduxjs/toolkit";
import { IStore } from "../../typings/IStore";

const locationSelector = (state: IStore) => state.router.location;

const pathnameSelector = createSelector(
    [locationSelector],
    location => location.pathname
);

const routerSearchSelector = createSelector(
    [locationSelector],
    location => location.search
);

export default {
    routerSearchSelector,
    pathnameSelector
}
