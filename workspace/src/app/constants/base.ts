import { BrandingProps } from "../views/plugins/plugin.types";
import { pluginRegistry, SUPPORTED_PLUGINS } from "../views/plugins/PluginRegistry";

const branding = (): BrandingProps => {
    const brandingObject = pluginRegistry.pluginComponent<BrandingProps>(SUPPORTED_PLUGINS.DI_BRANDING);
    return brandingObject
        ? brandingObject
        : {
              applicationName: "Silk",
              applicationCorporationName: "",
              applicationSuiteName: "",
              applicationDocumentationServiceUrl: "",
          };
};

export const APPLICATION_CORPORATION_NAME = () => branding().applicationCorporationName;

export const APPLICATION_SUITE_NAME = () => branding().applicationSuiteName;

export const APPLICATION_NAME = () => branding().applicationName;

export const APPLICATION_DOCUMENTATION_SERVICE_URL = () => branding().applicationDocumentationServiceUrl;
