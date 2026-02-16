import { Notification, Spacing } from "@eccenca/gui-elements";
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

    return (
        <>
            <Notification intent="warning">
                {t("common.messages.deprecatedPluginBanner")}
                {`${t("common.messages.deprecatedPluginBanner")}`}
                <Spacing size="small" />

                {deprecatedPlugin[0].deprecationMessage && <strong>{deprecatedPlugin[0].deprecationMessage}</strong>}
            </Notification>
            <br />
        </>
    );
}
