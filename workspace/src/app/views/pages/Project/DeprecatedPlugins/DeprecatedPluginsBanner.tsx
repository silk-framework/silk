import { HtmlContentBlock, Notification, Spacing } from "@eccenca/gui-elements";
import React, { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { DeprecatedPluginsModel } from "views/pages/DeprecatedPlugins";
import useErrorHandler from "../../../../hooks/useErrorHandler";
import { requestDeprecatedPlugins } from "@ducks/common/requests";

// Banner component to display a warning message if deprecated plugins are used in the current project/task, encouraging users to replace them with supported alternatives.
export default function DeprecatedPluginsBanner({ projectId, taskId }: { projectId: string; taskId: string }) {
    const [t] = useTranslation();
    const [deprecatedPlugin, setDeprecatedPlugin] = useState<DeprecatedPluginsModel[]>([]);
    const { registerError } = useErrorHandler();

    useEffect(() => {
        requestDeprecatedPlugins(projectId, taskId)
            .then((response) => {
                setDeprecatedPlugin(response.data);
            })
            .catch((error) => {
                registerError(
                    "DeprecatedPlugins_FetchPlugins",
                    t("pages.deprecatedPlugins.errors.fetchPlugins"),
                    error,
                );
            });

        return () => {};
    }, [projectId, taskId]);

    if (deprecatedPlugin.length === 0) return null;

    const plugin = deprecatedPlugin[0];

    return (
        <>
            <Notification intent="warning">
                <HtmlContentBlock>
                    <p>{t("common.messages.deprecatedPluginBanner")}</p>
                    <ul>
                        <li>
                            <strong>{t("common.messages.deprecatedPluginBannerPluginLabel")}:</strong> {plugin.pluginLabel}
                        </li>
                        <li>
                            <strong>{t("common.messages.deprecatedPluginBannerTaskLabel")}:</strong> {plugin.linkLabel}
                        </li>
                        {plugin.deprecationMessage && (
                            <li>
                                <strong>{t("common.messages.deprecatedPluginBannerDeprecationMessage")}:</strong>{" "}
                                {plugin.deprecationMessage}
                            </li>
                        )}
                    </ul>
                </HtmlContentBlock>
            </Notification>
            <Spacing />
        </>
    );
}
