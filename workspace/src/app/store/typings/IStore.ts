import { ICommonState } from "@ducks/common/typings";
import { IErrorState } from "@ducks/error/typings";
import { IWorkspaceState } from "@ducks/workspace/typings";
import { RouterState } from "connected-react-router";

export interface IStore {
    common: ICommonState;
    workspace: IWorkspaceState;
    router: RouterState;
    error: IErrorState;
}
