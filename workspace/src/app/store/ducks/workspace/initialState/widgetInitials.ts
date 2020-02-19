import { IPrefixState, IWidgetsState } from "@ducks/workspace/typings";

export function initialNewPrefixState(): IPrefixState {
    return {
        prefixName: '',
        prefixUri: ''
    }
}

export function initialConfigurationState() {
    return {
        prefixes: [],
        newPrefix: initialNewPrefixState(),
        isLoading: false,
        error: {}
    }
}

export function initialWarningItemState() {
    return {
        taskId: "",
        errorSummary: "",
        taskLabel: "",
        errorMessage: "",
        stackTrace: {
            errorMessage: "",
            lines: []
        }
    }

}

export function initialWarningState() {
    return {
        results: [],
        isLoading: false,
        error: {}
    }
}

export function initialFilesState() {
    return {
        results: [],
        isLoading: false,
        error: {}
    }
}

export function initialWidgetsState(): IWidgetsState {
    return {
        configuration: initialConfigurationState(),
        warnings: initialWarningState(),
        files: initialFilesState()
    }
}
