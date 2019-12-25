import { IDashboardState} from "@ducks/dashboard/typings";
import { IGlobalState } from "@ducks/global/typings";
import { RouterState } from "connected-react-router";

export interface IStore {
    global: IGlobalState;
    dashboard: IDashboardState;
    router: RouterState;
}
