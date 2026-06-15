import { IWidgetsState } from "@ducks/workspace/typings";

export function initialWarningItemState() {
    return {
        taskId: "",
        errorSummary: "",
        taskLabel: "",
        errorMessage: "",
        stackTrace: {
            errorMessage: "",
            lines: [],
        },
    };
}

export function initialWarningState() {
    return {
        results: [],
        isLoading: false,
        error: {},
    };
}

export function initialFilesState() {
    return {
        results: [],
        isLoading: false,
        error: {},
    };
}

export function initialWidgetsState(): IWidgetsState {
    return {
        warnings: initialWarningState(),
        files: initialFilesState(),
    };
}
