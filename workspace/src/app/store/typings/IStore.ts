import { IWorkspaceState } from "@ducks/workspace/typings";
import { ICommonState } from "@ducks/common/typings";
import { RouterState } from "connected-react-router";
import { IErrorState } from "@ducks/error/typings";

export interface IStore {
    common: ICommonState;
    workspace: IWorkspaceState;
    router: RouterState;
    error: IErrorState;
}
