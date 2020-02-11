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

export function initialWidgetsState(): IWidgetsState {
    return {
        configuration: initialConfigurationState()
    }
}
