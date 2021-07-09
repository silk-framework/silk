import { ErrorResponse, FetchError } from "../../../services/fetch/responseInterceptor";

export type DIErrorTypes = Error | FetchError | ErrorResponse;

/**** Error format for all registered failures withing DI ****/
export interface DIErrorFormat {
    /**** Globally unique Id that has to be set by the developer. Convention: "<ComponentName>_<ActionName>", e.g. "WorkflowEditor_LoadWorkflow". */
    id: string;

    /**** Human readable error message. ****/
    message: string;

    /**** Timestamp of when the error has been registered. *****/
    timestamp: number;

    /**** Optional Error object explaining the exception. ****/
    cause: DIErrorTypes | null;
}

/****** DI error state containing all errors ******/
export interface IErrorState {
    errors: Array<DIErrorFormat>;
}
