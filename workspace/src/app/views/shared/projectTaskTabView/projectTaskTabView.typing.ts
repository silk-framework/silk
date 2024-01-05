export interface PartialSourcePathAutoCompletionRequest extends AutoCompletionRequest {
    /** If the path that should be auto-completed is an object path, i.e. selecting entities/objects instead of literal values. */
    isObjectPath?: boolean
    /** Optional workflow context. */
    workflowTaskContext?: TaskContext
}

export interface AutoCompletionRequest {
    /** The input string of the auto-completion. */
    inputString: string
    /** The cursor position inside the input string. */
    cursorPosition: number
    /** Optional max. suggestions that should be returned to limit the number of results */
    maxSuggestions?: number
}

/** Defines the context a task is used in, e.g. in a workflow. */
export interface TaskContext {
    /** The input configuration of the task inside the workflow. */
    inputTasks?: TaskContextInputTask[]
    outputTasks?: TaskContextOutputTask[]
}

interface TaskContextTask {
    /** The ID of the task */
    id: string
}

export interface TaskContextInputTask extends TaskContextTask {}

export interface TaskContextOutputTask extends TaskContextTask {
    /** True if connection is made to the config port of the output task. */
    configPort: boolean
    /** The data input port index of the output task that is connected. */
    inputPort?: number
}
