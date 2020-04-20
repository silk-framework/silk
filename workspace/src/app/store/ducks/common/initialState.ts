import { IArtefactItem, IArtefactModal, ICommonState } from "./typings";
import { getLocale } from "./thunks/locale.thunk";

export function initialArtefactModalState(): IArtefactModal {
    return {
        isOpen: false,
        artefactsList: [],
        selectedArtefact: {} as IArtefactItem,
        cachedArtefactProperties: {},
        selectedDType: 'all',
        loading: false,
    }
}

export function initialCommonState(): ICommonState {
    return {
        locale: getLocale(),
        currentProjectId: null,
        authenticated: true,
        searchQuery: '',
        error: {},
        availableDataTypes: {},
        initialSettings: {},
        artefactModal: initialArtefactModalState()
    }
}
