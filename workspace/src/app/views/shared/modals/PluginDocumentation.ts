import { IPluginOverview } from "@ducks/common/typings";

/** Documentation displayed for a plugin in configuration and rule-editor views. */
export interface PluginDocumentation {
    key: string;
    title?: string;
    description?: string;
    markdownDocumentation?: string;
    namedAnchor?: string;
    relatedPlugins?: RelatedPluginDocumentation[];
}

export interface RelatedPluginDocumentation {
    plugin: IPluginOverview;
    description: string;
}
