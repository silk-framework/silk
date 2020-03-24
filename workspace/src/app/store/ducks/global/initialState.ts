import { IGlobalState } from "./typings";
import { getLocale } from "./thunks/locale.thunk";

export function initialGlobalState(): IGlobalState {
    return {
        locale: getLocale(),
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
