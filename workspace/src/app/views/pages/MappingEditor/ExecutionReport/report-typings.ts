export interface ExecutionReportType {
    task: {
        id: string;
    };
    summary: Array<any>;
    warnings: Array<any>;
}

export interface ExecutionReportProps {
    //project Id
    project: string;
    //task Id
    task: string;
    /**
     * control that dictates the frequency of updates for the execution report
     */
    updateCounter: number;
    /*
     * DI store object that provides the business layer API to DI related services
     */
    diStore: {
        getLinkingExecutionReport: (project: string, task: string) => Promise<ExecutionReportType>;
        getTransformExecutionReport: () => Promise<ExecutionReportProps>;
    };
}
