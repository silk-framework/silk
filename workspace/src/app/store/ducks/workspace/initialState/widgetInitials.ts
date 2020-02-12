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
        newPrefix: initialNewPrefixState()
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
    }
}

export function initialWidgetsState(): IWidgetsState {
    return {
        configuration: initialConfigurationState(),
        warnings: initialWarningState()
    }
}
