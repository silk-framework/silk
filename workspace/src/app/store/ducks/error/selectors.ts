import { IErrorState } from "./typings";
import {IStore} from "../../typings/IStore";

//error slice state
const errorSelector = (state: IStore): IErrorState => state.error;

export default errorSelector;
