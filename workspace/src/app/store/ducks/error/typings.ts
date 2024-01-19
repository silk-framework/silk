import { ErrorResponse, FetchError } from "../../../services/fetch/responseInterceptor";

export type DIErrorTypes = Error | FetchError | ErrorResponse;

/** Returns the error message of the error if any. */
export const diErrorMessage = (error?: DIErrorTypes | null): string | undefined | null => {
    if (!error) {
        return error;
    } else {
        return (error as Error).message || (error as ErrorResponse).detail;
    }
};

/** Error format for all registered failures within DI. */
export interface DIErrorFormat {
    /** Globally unique ID that has to be set by the developer. Convention: "<ComponentName>_<ActionName>", e.g. "WorkflowEditor_LoadWorkflow".
     * There is always only the last error with the same ID displayed in the error overview. */
    id: string;

    /** Human readable error message. */
    message: string;

    /** Timestamp of when the error has been registered. */
    timestamp: number;

    /** Optional Error object explaining the exception. */
    cause: DIErrorTypes | null;

    /** If for some reason an error should not be displayed with "danger" intent this can be changed here. */
    alternativeIntent?: "warning";
}

/** DI error state containing all errors */
export interface IErrorState {
    errors: Array<ApplicationError>;
}

export type ApplicationError = DIErrorFormat & { errorNotificationInstanceId?: string };
