import { IWorkspaceState } from "@ducks/workspace/typings";
import { ICommonState } from "@ducks/common/typings";
import { RouterState } from "connected-react-router";
import { IDatasetState } from "@ducks/dataset/typings";
import { IErrorState } from "@ducks/error/typings";

export interface IStore {
    common: ICommonState;
    workspace: IWorkspaceState;
    dataset: IDatasetState;
    router: RouterState;
    error: IErrorState;
}
