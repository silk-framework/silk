import { IStore } from "store/typings/IStore";
import { IErrorState } from "./typings";

//error slice state
const errorSelector = (state: IStore): IErrorState => state.error;

export default errorSelector;
