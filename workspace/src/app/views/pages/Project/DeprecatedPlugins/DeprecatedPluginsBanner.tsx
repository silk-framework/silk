import React, { useEffect, useState } from "react";
import useErrorHandler from "../../../../hooks/useErrorHandler";
import { useTranslation } from "react-i18next";
import { DeprecatedPluginsModel } from "views/pages/DeprecatedPlugins";
import { coreApi } from "../../../../utils/getApiEndpoint";
import { fetch } from "../../../../services/fetch/fetch";
import { Notification } from "@eccenca/gui-elements";

export default function DeprecatedPluginsBanner({ projectId, taskId }: { projectId: string; taskId: string }) {
    const [t] = useTranslation();
    const [isDeprecated, setIsDeprecated] = useState(false);
    const { registerError } = useErrorHandler();

    useEffect(() => {
        const fetchDeprecatedPlugins = async () => {
            try {
                const response = await fetch<DeprecatedPluginsModel[]>({
                    url: coreApi("/usages/deprecatedPlugins"),
                    query: { project: projectId, task: taskId },
                    method: "GET",
                });
                setIsDeprecated(response.data.length > 0);
            } catch (error) {
                registerError(
                    "DeprecatedPlugins_FetchPlugins",
                    t("pages.deprecatedPlugins.errors.fetchPlugins", "Failed to load deprecated plugins"),
                    error,
                );
            }
        };

        // Initial fetch
        fetchDeprecatedPlugins();

        return () => {};
    }, [projectId, taskId, t, registerError]);

    if (!isDeprecated) return null;

    return (
        <>
            <Notification intent="warning">
                {t(
                    "common.messages.deprecatedPluginBanner",
                    "This plugin is deprecated and will be removed in the future. Please exchange it for a supported plugin as soon as possible.",
                )}
            </Notification>
            <br />
        </>
    );
}
