import { Card, CardContent, CardHeader, CardTitle, Divider, Notification, Spacing } from "@eccenca/gui-elements";
import useErrorHandler from "../../../hooks/useErrorHandler";
import React, { useState, useEffect } from "react";
import { useTranslation } from "react-i18next";
import { DeprecatedPluginsModel } from "../DeprecatedPlugins";
import { coreApi } from "../../../utils/getApiEndpoint";
import { fetch } from "../../../services/fetch/fetch";

type DeprecatedPluginsProps = { projectId: string; taskId?: never } | { projectId?: never; taskId: string };

export function DeprecatedPluginWidget({ projectId, taskId }: DeprecatedPluginsProps) {
    const [t] = useTranslation();
    const [deprecatedPlugins, setDeprecatedPlugins] = useState<DeprecatedPluginsModel[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const { registerError } = useErrorHandler();

    useEffect(() => {
        const fetchDeprecatedPlugins = async () => {
            try {
                const response = await fetch<DeprecatedPluginsModel[]>({
                    url: coreApi("/usages/deprecatedPlugins"),
                    method: "GET",
                });
                setDeprecatedPlugins(response.data);
            } catch (error) {
                registerError(
                    "DeprecatedPlugins_FetchPlugins",
                    t("pages.deprecatedPlugins.errors.fetchPlugins", "Failed to load deprecated plugins"),
                    error,
                );
            } finally {
                setIsLoading(false);
            }
        };

        fetchDeprecatedPlugins();
    }, []);

    const deprecatedPluginsOfProjectOrTask = projectId
        ? deprecatedPlugins.filter((plugin) => plugin.project === projectId || plugin.task === taskId)
        : deprecatedPlugins;

    if (isLoading || deprecatedPluginsOfProjectOrTask.length === 0) {
        return null;
    }

    return (
        <Card>
            <CardHeader>
                <CardTitle>
                    <h2>
                        {t("widget.deprecatedPluginWidget.title", "Deprecated Plugins")} (
                        {deprecatedPluginsOfProjectOrTask.length})
                    </h2>
                </CardTitle>
            </CardHeader>
            <Divider />
            <CardContent>
                <p>{deprecatedPluginsOfProjectOrTask.map((plugin) => plugin.pluginLabel).join(", ")}</p>
                <Spacing />
                <Notification intent="warning">
                    {t(
                        "widget.deprecatedPluginWidget.message",
                        "The above listed plugins are deprecated and will be removed in future versions. Please check the documentation for alternatives.",
                    )}
                </Notification>
            </CardContent>
        </Card>
    );
}
