import { Reducer } from "redux";
import { IDashboardState, initialDashboardState } from "./ducks/dashboard/dtos";
import { IGlobalState, initialGlobalState } from "./ducks/global/dtos";

export interface IStore {
    global: IGlobalState;
    dashboard: IDashboardState;
    router: Reducer<any, any>;
}

export function initialStore() {
    return {
        global: initialGlobalState(),
        dashboard: initialDashboardState()
    }
}
