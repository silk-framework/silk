/****  all pages/components with errors in the error Redux state  ****/
export enum ERROR_HANDLED_SECTIONS {
    workflowEditor = "workflowEditor",
}

/**** Error format for all registered failures withing DI ****/
export interface DIErrorFormat {
    /**** Human readable error message registered with error handler hook ****/
    message: string;

    /**** time instance of error in milliseconds *****/
    timestamp: number;

    /**** Optional stack trace explaining the exception ****/
    cause?: Error;
}

/****** DI error state containing all errors grouped
 *  by an Id i.e the unique section of the application e.g "workflowEditor", "RelatedItems" etc
 *  the state is an object of sectioned errors by Id
 ******/
export interface IErrorState {
    workflowEditor: {
        [errorId: string]: DIErrorFormat;
    };
}
