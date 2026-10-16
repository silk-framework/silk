import { IArtefactModal, ICommonState } from "./typings";
import i18n from "../../../../language";

export function initialArtefactModalState(): IArtefactModal {
    return {
        isOpen: false,
        error: {},
        artefactsList: [],
        selectedArtefact: undefined,
        cachedArtefactProperties: {},
        selectedDType: "all",
        loading: false,
        categories: [],
        info: undefined,
    };
}

export function initialCommonState(): ICommonState {
    return {
        userMenuDisplay: false,
        notificationMenuDisplay: false,
        locale: i18n.resolvedLanguage ?? "en",
        currentProjectId: undefined,
        currentTaskId: undefined,
        authenticated: true,
        searchQuery: "",
        error: {},
        availableDataTypes: {},
        initialSettings: {
            emptyWorkspace: true,
            initialLanguage: "en",
            hotKeys: {},
            templatingEnabled: false,
            assistantSupported: false,
            companion: {
                enabled: false,
                apiBasePath: "/dataplatform/api/companion",
                streamPath: "/dataplatform/companion-websocket",
            },
            mappingCreatorEnabled: false,
            aclEnabled: false,
        },
        exportTypes: [],
        artefactModal: initialArtefactModalState(),
        taskPluginOverviews: [],
    };
}
