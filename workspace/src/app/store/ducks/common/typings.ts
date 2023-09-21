import {
    IAutocompleteDefaultResponse,
    IMetadata,
    IProjectTask,
    PluginType,
    TaskType,
    TemplateValueType,
} from "@ducks/shared/typings";
import { IRenderModifiers } from "@eccenca/gui-elements/src/components/AutocompleteField/AutoCompleteField";
import { OptionallyLabelledParameter } from "../../../views/taskViews/linking/linking.types";

export interface IAvailableDataTypes {
    [key: string]: IAvailableDataType;
}

export interface IAvailableDataTypeOption {
    id: string;
    label: string;
}

export interface IAvailableDataType {
    label: string;
    field: string;
    options: IAvailableDataTypeOption[];
}

/** Extensions to the auto-completion config on the frontend side. */
interface AutoCompletionFrontendExtensions {
    /** Optional function if the auto-completion results are not coming from the standard plugin parameter auto-completion endpoint. */
    customAutoCompletionRequest?: (
        textQuery: string,
        limit: number
    ) => IAutocompleteDefaultResponse[] | Promise<IAutocompleteDefaultResponse[]>;
    /** Custom item renderer. By default the item label is displayed. */
    customItemRenderer?: (
        autoCompleteResponse: IAutocompleteDefaultResponse,
        query: string,
        modifiers: IRenderModifiers,
        handleSelectClick: () => any
    ) => string | JSX.Element;
}

/** Properties for parameter auto-completion. */
export interface IPropertyAutocomplete extends AutoCompletionFrontendExtensions {
    /** If the parameter only allows values coming from the auto-completion, i.e. no custom values are allowed. */
    allowOnlyAutoCompletedValues: boolean;
    /** If there are optional labels connected to the values that will be requested. */
    autoCompleteValueWithLabels: boolean;
    /** The parameter IDs on the same level that this parameter's auto-completion depends on, e.g. project resource auto-completion depends on the project parameter. */
    autoCompletionDependsOnParameters: string[];
}

type ValidPropertyJsonSchemaType = "string" | "object";

/** Description of a parameter of an item. */
export interface IArtefactItemProperty {
    title: string;
    description: string;
    // Either "string" or "object"
    type: ValidPropertyJsonSchemaType;
    // The default value
    value: OptionallyLabelledParameter<string> | null;
    advanced: boolean;
    parameterType: string;
    visibleInDialog: boolean;
    autoCompletion?: IPropertyAutocomplete;
    // in case of type=="object" this will be defined
    pluginId?: string;
    // in case of type=="object" this will be defined
    properties?: Record<string, IArtefactItemProperty>;
    // Lists the required parameters in case properties is defined
    required?: string[];
}

/** Parameter of a task. */
export interface ITaskParameter {
    paramId: string;
    param: IArtefactItemProperty;
}

export type IOverviewArtefactItemList = Record<string, IOverviewArtefactItem>;

export interface IOverviewArtefactItem {
    title: string;
    description: string;
    taskType: TaskType;
    categories: string[];
    markdownDocumentation?: string;
}

export type DistanceMeasureRange = "boolean" | "unbounded" | "normalized";
/** The full task plugin description, including detailed schema. */
export interface IPluginDetails {
    title: string;
    description: string;
    taskType: TaskType;
    type: "object";
    categories: string[];
    distanceMeasureRange?: DistanceMeasureRange;
    properties: {
        [key: string]: IArtefactItemProperty;
    };
    required: string[];
    pluginId: string;
    pluginType?: PluginType;
    markdownDocumentation?: string;
    autoConfigurable?: boolean;
}

/** Overview version of an item description. */
export interface IPluginOverview {
    key: string;
    taskType?: TaskType;
    title?: string;
    description?: string;
    categories?: string[];
    markdownDocumentation?: string;
    /** Plugin icon in Data URL format. */
    pluginIcon?: string;
}

export type AlternativeTaskUpdateFunction = (
    projectId: string,
    taskId: string,
    parameterData: object,
    variableTemplateData: object,
    dataParameters: Record<string, string> | undefined
) => any | Promise<any>;

/** Contains all data that is needed to render an update dialog. */
export interface IProjectTaskUpdatePayload {
    projectId: string;
    taskId: string;
    taskPluginDetails: IPluginDetails;
    /** Needed to display the task label in the modal title. */
    metaData: IMetadata;
    currentParameterValues: {
        [key: string]: string | object;
    };
    currentTemplateValues: TemplateValueType;
    dataParameters?: {
        [key: string]: string;
    };
    /** Function that is called instead of the task PATCH endpoint. */
    alternativeUpdateFunction?: AlternativeTaskUpdateFunction;
    /** The title of the modal that will be displayed */
    alternativeTitle?: string;
}

export type MetaDataFactoryFunction = (pluginDetails: IPluginDetails) => IMetadata;

/** Contains additional data to pre-set some configs or parameters when creating a new task. */
export interface TaskPreConfiguration {
    /** Meta data to pre-configure the label or description of a task. */
    metaData?: IMetadata;
    /** Allows to set parameters of the task to be created, e.g. file resource or input source. */
    preConfiguredParameterValues?: {
        [key: string]: string | object;
    };
    /** When this is set the modal starts directly in the create-task step. */
    taskPluginId?: string;
    /** If it should be possible to create a project. */
    showProjectItem?: boolean;
    /** If set to false, then the widget to change the project for the task will not be shown. Default: false */
    showProjectChangeWidget?: boolean;
    /** Blacklist some plugins that should not be possible to create. */
    pluginBlackList?: string[];
    /** If this is set, then instead of redirecting to the newly created task, this function is called. */
    alternativeCallback?: (newTask: IProjectTask) => any;
    /** A factory function that should be called to generate meta data based on the selected plugin. */
    metaDataFactoryFunction?: MetaDataFactoryFunction;
}

export interface ProjectTaskDownloadInfo {
    downloadSupported: boolean;
    info: string;
}

export interface IExportTypes {
    description: string;
    fileExtension: string;
    id: string;
    label: string;
    mediaType: string;
}

/** State of the artefact modal. */
export interface IArtefactModal {
    // If true, this modal is shown to the user
    isOpen: boolean;

    loading: boolean;

    // The list of item types that can be selected.
    artefactsList: IPluginOverview[];

    categories: {
        label: string;
        count: number;
    }[];

    // The selected item type
    selectedArtefact?: IPluginOverview;

    // cached plugin descriptions
    cachedArtefactProperties: {
        [key: string]: IPluginDetails;
    };

    // The selected item category
    selectedDType: string;

    // If an existing task should be updated
    updateExistingTask?: IProjectTaskUpdatePayload;

    /** Allows to pre-set some of the task parameters or meta data when creating a new task. */
    newTaskPreConfiguration?: TaskPreConfiguration;

    error: any;
}

export interface ICommonState {
    /**
     * Used in Project details page only and store the current selected project id
     * Received from router
     */
    currentProjectId: string | undefined;
    currentTaskId: string | undefined;
    locale: string;
    initialSettings: IInitFrontend;
    /** The task plugin overview information. */
    taskPluginOverviews: IPluginOverview[];
    authenticated: boolean;
    searchQuery: string;
    error?: any;
    availableDataTypes: IAvailableDataTypes;
    exportTypes: IExportTypes[];
    artefactModal: IArtefactModal;
}

/** Config information from the backend to initialize the frontend. */
export interface IInitFrontend {
    /**
     * If the workspace if currently empty, i.e. has no projects in it.
     */
    emptyWorkspace: boolean;
    /**
     * Initial language from backend. This can be "overwritten" by the user via the UI.
     */
    initialLanguage: string;

    /** The max. file upload size in bytes supported by the backend. */
    maxFileUploadSize?: number;

    /**
     * DM url, in case of missing, hide navigation bar
     */
    dmBaseUrl?: string;

    /** Configured links to the DM that should be displayed in the global navigation menu. */
    dmModuleLinks?: IDmLink[];

    /** Hotkey configuration for the frontend. */
    hotKeys: Partial<Record<HotKeyIds, string>>;

    /** The application version. */
    version?: string;

    /** If templating in some input fields is enabled. */
    templatingEnabled: boolean;
}

type HotKeyIds = "quickSearch" | "overview";

export interface IDmLink {
    path: string;
    defaultLabel: string;
    icon?: string;
}
