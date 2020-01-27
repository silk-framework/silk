import { IWorkspaceState} from "@ducks/workspace/typings";
import { IGlobalState } from "@ducks/global/typings";
import { RouterState } from "connected-react-router";
import { IDatasetState } from "@ducks/dataset/typings";

export interface IStore {
    global: IGlobalState;
    workspace: IWorkspaceState;
    dataset: IDatasetState;
    router: RouterState;
}
