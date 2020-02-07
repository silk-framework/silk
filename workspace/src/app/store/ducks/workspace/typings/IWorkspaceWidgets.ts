export interface IPrefixState {
    /**
     * Name of prefix
     */
    prefixName: string;
    /**
     * Name of prefix Uri
     */
    prefixUri: string;
}

export interface IWorkspaceConfigurationWidget {
    /**
     * Array of prefixes List
     */
    prefixes: IPrefixState[];
    /**
     * Plain object  for new prefix
     */
    newPrefix: IPrefixState;
}

export interface IWidgetsState {
    /**
     * Store Project details page all widgets by widget name
     */
    configuration: IWorkspaceConfigurationWidget;
}
