import { Card, CardContent, CardHeader, CardTitle, Divider, Link, Notification, Spacing } from "@eccenca/gui-elements";
import useErrorHandler from "../../../../hooks/useErrorHandler";
import { contextualPath } from "../../../../constants/path";
import React, { useState, useEffect } from "react";
import { useTranslation } from "react-i18next";
import { DeprecatedPluginsModel } from "../../DeprecatedPlugins";
import { requestDeprecatedPlugins } from "@ducks/common/requests";

type DeprecatedPluginsProps = { projectId: string; taskId?: never } | { projectId?: never; taskId: string };

type GroupedTask = {
    label: string;
    link: string;
};

type GroupedDataWithLinks = {
    pluginLabel: string;
    tasks: GroupedTask[];
};

export function DeprecatedPluginsWidget({ projectId, taskId }: DeprecatedPluginsProps) {
    const [t] = useTranslation();
    const [deprecatedPlugins, setDeprecatedPlugins] = useState<DeprecatedPluginsModel[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const { registerError } = useErrorHandler();

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

    function groupByPluginWithLinks(data: DeprecatedPluginsModel[]): GroupedDataWithLinks[] {
        const groupedMap: Record<string, GroupedTask[]> = {};

        data.forEach((item) => {
            if (!groupedMap[item.pluginLabel]) {
                groupedMap[item.pluginLabel] = [];
            }
            groupedMap[item.pluginLabel].push({
                label: item.taskLabel,
                link: item.link,
            });
        });

        return Object.entries(groupedMap).map(([pluginLabel, tasks]) => ({
            pluginLabel,
            tasks,
        }));
    }

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
                <ul style={{ display: "flex", flexDirection: "column", gap: "0.2rem" }}>
                    {groupByPluginWithLinks(deprecatedPlugins).map((plugin) => (
                        <li key={plugin.pluginLabel} style={{ display: "flex", flexDirection: "row", gap: "0.2rem" }}>
                            <p style={{}}>{plugin.pluginLabel}:</p>
                            {plugin.tasks.map((task, index) => (
                                <span key={task.link}>
                                    <Link href={contextualPath(task.link)}>{task.label}</Link>
                                    {index < plugin.tasks.length - 1 ? ", " : ""}
                                </span>
                            ))}
                        </li>
                    ))}
                </ul>
                <Spacing />
                <Notification intent="warning">{t("widget.deprecatedPluginWidget.message")}</Notification>
            </CardContent>
        </Card>
    );
}
