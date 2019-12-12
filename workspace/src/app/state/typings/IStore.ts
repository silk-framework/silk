import { Reducer } from "redux";
import { IDashboardState} from "../ducks/dashboard/typings";
import { IGlobalState } from "../ducks/global/typings";

export interface IStore {
    global: IGlobalState;
    dashboard: IDashboardState;
    router: Reducer<any, any>;
}
