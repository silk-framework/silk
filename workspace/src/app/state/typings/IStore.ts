import { Reducer } from "redux";
import { IDashboardState} from "../ducks/dashboard/typings";
import { IGlobalState } from "../ducks/global/dtos";

export interface IStore {
    global: IGlobalState;
    dashboard: IDashboardState;
    router: Reducer<any, any>;
}
