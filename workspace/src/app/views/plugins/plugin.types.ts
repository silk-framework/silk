import { IArtefactItemProperty } from "@ducks/common/typings";
import { IViewActions } from "../../views/plugins/PluginRegistry";
import { TestableComponent } from "@eccenca/gui-elements";

export type IPreview = IDatasetConfigPreview | IResourcePreview | IDatasetPreview | FixedPreview;

interface IValidation {
    validate: () => boolean | Promise<boolean>;
    errorMessage: string;
}

export interface TaskParameters {
    [key: string]: string | TaskParameters;
}

/**
 * The dataset configuration.
 */
export interface IDatasetInfo {
    /** The plugin ID, e.g. 'csv'. */
    type: string;
    /** The parameters of the plugin. */
    parameters: TaskParameters;
}

/**
 * A file resource preview request. Usually this should not be used and a full dataset config should be given.
 */
export interface IResourcePreview {
    /** The project ID of the project the resource is located in. */
    project: string;
    /** The resource name of an existing project resource. */
    resource: string;
}

/**
 * A dataset preview request.
 */
export interface IDatasetConfigPreview {
    /** The project ID. Referenced resources must be located in this project. */
    project: string;
    datasetInfo: IDatasetInfo;
}

/** A preview request for an existing dataset */
export interface IDatasetPreview {
    project: string;
    /** The dataset ID */
    dataset: string;
    /** The type, in case of multi-type datasets like RDF, JSON etc. are used. If not specified it uses the "default" type of the dataset.  */
    typeUri?: string;
}

export interface DatasetPreviewResourceTextPayload {
    project: string;
    datasetId: string;
    datasetInfo?: IDatasetInfo;
    offset: number;
    limit: number;
}

export interface DatasetPreviewResourceTextResponse {
    text: string;
    endReached: boolean;
    isTextBased: boolean;
    mimeType: string;
    charsRead: number;
    endOfLineReached: true;
    percentage: number;
    fileSize: number;
}

/** Parameters of the data preview component. */
export interface DataPreviewProps extends TestableComponent {
    // The title of the widget
    title: string;
    // The preview configuration
    preview: IPreview;
    // Validation to check if a preview should be rendered
    externalValidation?: IValidation;
    // If defined this should be used to get the parameters for the dataset config.
    // Reason for use: In forms the data preview widget won't be re-rendered when form values change.
    datasetConfigValues?: () => TaskParameters;
    // If the data preview should be loaded automatically without user interaction. Default: false
    autoLoad?: boolean;
    // An optional ID for the preview widget
    id?: string;
    /** If initially the raw view should be shown. */
    startWithRawView?: boolean;
}

/** User menu footer component. */
export interface UserMenuFooterProps {
    version?: string;
}

/** Branding plugin values. */
export interface BrandingProps {
    // The company name of the application
    applicationCorporationName: string;
    // If Silk is part of a larger application suite, this should be set.
    applicationSuiteName: string;
    // The application name for Silk.
    applicationName: string;
}

/** Extended handling for parameter types. */
export interface ParameterExtensions {
    /** Extends the given parameter definition (or leaves it as it is). */
    extend: (input: IArtefactItemProperty) => IArtefactItemProperty;
}

/** Props for mapping suggestion. */
export interface SuggestionNGProps {
    // Project the transform task is in.
    projectId: string;
    // The transform task to create matches for.
    transformTaskId: string;
    // Generic actions and callbacks that could be necessary.
    viewActions?: IViewActions;
}

/** The preview gets a fixed list of types and their values. */
export interface FixedPreview {
    /** The list of possible types. */
    types: string[];

    /** The preview for each type. */
    typeValues: Map<string, PreviewContent>;
}

interface PreviewContent {
    /** property/attribute names. */
    attributes: string[];
    /** The values for each attribute. This must have the same length as the attributes array. */
    values: string[][];
}
