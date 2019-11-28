import { GlobalDto } from "./ducks/global/dtos";
import { connectRouter } from "connected-react-router";
import { Reducer } from "redux";
import { createBrowserHistory } from "history";
import { DashboardDto } from "./ducks/dashboard/dtos";

export class StoreDto {
    global: GlobalDto;
    dashboard: DashboardDto;
    router: Reducer<any, any>;

    constructor() {
        this.global = new GlobalDto();
        this.dashboard = new DashboardDto();
        this.router = connectRouter(createBrowserHistory())
    }

}
