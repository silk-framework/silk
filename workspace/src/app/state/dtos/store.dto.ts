import { GlobalDto } from "../ducks/global/dtos";
import { connectRouter } from "connected-react-router";
import { Reducer } from "redux";
import { createBrowserHistory } from "history";

export class StoreDto {
    global: GlobalDto;
    router: Reducer<any, any>;

    constructor() {
        this.global = new GlobalDto();
        this.router = connectRouter(createBrowserHistory())
    }

}
