import { IGlobalState } from "./typings";
import { getLocale } from "./thunks/locale.thunk";

export function initialGlobalState(): IGlobalState {
    return {
        locale: getLocale(),
        currentProjectId: null,
        authenticated: true,
        searchQuery: '',
        error: {},
        availableDataTypes: {},
        initialSettings: {},
        artefactModal: {
            isOpen: false,
            artefactsList: [],
            selectedArtefact: null,
            selectedDType: 'all'
        }
    }
}
