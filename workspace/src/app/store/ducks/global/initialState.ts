import { IGlobalState } from "./typings";
import { getLocale } from "./thunks/locale.thunk";

export function initialGlobalState(): IGlobalState {
    return {
        locale: getLocale(),
        currentProjectId: null,
        authenticated: true,
        searchQuery: '',
        breadcrumbs: [],
        error: {},
        availableDataTypes: {},
        artefactModal: {
            isOpen: false,
            artefactsList: [],
            selectedArtefact: null
        }
    }
}
