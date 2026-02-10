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
                    query: { project: projectId, task: taskId },
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

        // Initial fetch
        fetchDeprecatedPlugins();

        // Listen for workflow save events
        const handleWorkflowSaved = (event: CustomEvent) => {
            const { projectId: savedProjectId, taskId: savedTaskId } = event.detail;

            // Only refetch if it's relevant to this widget
            if (savedProjectId === projectId || savedTaskId === taskId) {
                fetchDeprecatedPlugins();
            }
        };

        window.addEventListener("workflow:saved", handleWorkflowSaved as EventListener);

        // Cleanup listener on unmount
        return () => {
            window.removeEventListener("workflow:saved", handleWorkflowSaved as EventListener);
        };
    }, [projectId, taskId, t, registerError]);

    if (isLoading || deprecatedPlugins.length === 0) {
        return null;
    }

    return (
        <Card>
            <CardHeader>
                <CardTitle>
                    <h2>
                        {t("widget.deprecatedPluginWidget.title", "Deprecated Plugins")} ({deprecatedPlugins.length})
                    </h2>
                </CardTitle>
            </CardHeader>
            <Divider />
            <CardContent>
                <p>{deprecatedPlugins.map((plugin) => plugin.pluginLabel).join(", ")}</p>
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
