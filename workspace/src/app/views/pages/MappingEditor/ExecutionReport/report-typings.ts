import { IProjectTask } from "@ducks/shared/typings";

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
    diStore?: {
        getLinkingExecutionReport: (project: string, task: string) => Promise<ExecutionReportResponse>;
        getTransformExecutionReport: () => Promise<ExecutionReportProps>;
    };
}

export interface ExecutionReportResponse {
    label: string;
    operationDesc: string;
    entityCount: number;
    isDone: boolean;
    warnings: string[];
    // A task execution summary given as key value pairs
    summary: { key: string; value: string }[];
    /** Execution error message. */
    error?: string;
    task: IProjectTask;
    outputEntitiesSample: OutputEntitiesSample[];
    /** Transform report specific property. */
    ruleResults?: RuleResults;
    executionReportContext?: ExecutionReportContext;
}

interface ExecutionReportContext {
    entityUriOutput?: boolean;
}

type RuleResults = Record<string, RuleResult>;
interface RuleResult {
    errorCount: number;
    sampleErrors: any[];
}

export interface OutputEntitiesSample {
    entities: SampleEntity[];
    /** The entity schema corresponding to the sample entities. */
    schema: SampleEntitySchema;
    /** Optional ID to better identify what these sample entities belong to, e.g. for tasks that output more than one entity table. */
    id?: string;
}

interface SampleEntity {
    uri: string;
    values: string[][];
}

interface SampleEntitySchema {
    typeUri: string;
    typePath: string;
    properties: string[];
}
