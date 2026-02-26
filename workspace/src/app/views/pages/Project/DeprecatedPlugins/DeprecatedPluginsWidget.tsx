import { Card, CardContent, CardHeader, CardTitle, Divider, Notification, Spacing } from "@eccenca/gui-elements";
import useErrorHandler from "../../../../hooks/useErrorHandler";
import React, { useState, useEffect } from "react";
import { useTranslation } from "react-i18next";
import { DeprecatedPluginsModel } from "../../DeprecatedPlugins";
import { DeprecatedPluginsList } from "../../DeprecatedPlugins/DeprecatedPluginsList";
import { requestDeprecatedPlugins } from "@ducks/common/requests";

export function DeprecatedPluginsWidget({ projectId, taskId }: { projectId?: string; taskId?: string }) {
    const [t] = useTranslation();
    const [deprecatedPlugins, setDeprecatedPlugins] = useState<DeprecatedPluginsModel[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const { registerError } = useErrorHandler();
    const path = window.location.pathname;
    const isOnLinkingOrTransformPage = /\/projects\/[^/]+\/(linking|transform)\/[^/]+/.test(path);

    const fetchDeprecatedPlugins = () => {
        requestDeprecatedPlugins(projectId, taskId)
            .then((response) => {
                setDeprecatedPlugins(response.data);
                setIsLoading(false);
            })
            .catch((error) => {
                registerError(
                    "DeprecatedPluginsWidget_FetchPlugins",
                    t("pages.deprecatedPlugins.errors.fetchPlugins"),
                    error,
                );
                setIsLoading(false);
            })
            .finally(() => {
                setIsLoading(false);
            });
    };

    useEffect(() => {
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
    }, [projectId, taskId]);

    if (isLoading || deprecatedPlugins.length === 0) {
        return null;
    }

    const visiblePlugins = isOnLinkingOrTransformPage
        ? deprecatedPlugins.filter((plugin) => !path.startsWith((plugin.link || "").split("?")[0]))
        : deprecatedPlugins;

    return (
        <Card>
            <CardHeader>
                <CardTitle>
                    <h2>
                        {t("widget.deprecatedPluginWidget.title")} ({deprecatedPlugins.length})
                    </h2>
                </CardTitle>
            </CardHeader>
            <Divider />
            <CardContent>
                <Notification intent="warning">{t("widget.deprecatedPluginWidget.message")}</Notification>
                <Spacing />
                <DeprecatedPluginsList
                    filteredPlugins={visiblePlugins}
                    selectedPlugin={null}
                    selectedPluginKey={null}
                    isLoading={false}
                    hasCardWrapper={false}
                />
            </CardContent>
        </Card>
    );
}
