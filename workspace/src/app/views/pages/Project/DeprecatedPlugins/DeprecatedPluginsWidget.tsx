import { Card, CardContent, CardHeader, CardTitle, Divider, Link, Notification, Spacing } from "@eccenca/gui-elements";
import useErrorHandler from "../../../../hooks/useErrorHandler";
import { contextualPath } from "../../../../constants/path";
import React, { useState, useEffect } from "react";
import { useTranslation } from "react-i18next";
import { DeprecatedPluginsModel } from "../../DeprecatedPlugins";
import { requestDeprecatedPlugins } from "@ducks/common/requests";

type GroupedTask = {
    label: string;
    link: string;
};

type GroupedDataWithLinks = {
    pluginLabel: string;
    tasks: GroupedTask[];
    deprecationMessage: string;
};

export function DeprecatedPluginsWidget({ projectId, taskId }: { projectId?: string; taskId?: string }) {
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
            deprecationMessage: data.find((item) => item.pluginLabel === pluginLabel)?.deprecationMessage || "",
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
                <div style={{ display: "flex", flexDirection: "column", gap: "0.75rem" }}>
                    {groupByPluginWithLinks(deprecatedPlugins).map((plugin) => (
                        <div
                            key={plugin.pluginLabel}
                            style={{
                                display: "flex",
                                flexDirection: "column",
                                gap: "0.35rem",
                                borderLeft: "3px solid var(--eccenca-color-warning, #f5a623)",
                                paddingLeft: "0.75rem",
                            }}
                        >
                            <p style={{ fontWeight: 700, margin: 0 }}>
                                {plugin.pluginLabel}
                                <span
                                    style={{
                                        fontWeight: 400,
                                        color: "var(--eccenca-color-greyscale-medium, #888)",
                                        marginLeft: "0.3rem",
                                    }}
                                >
                                    ({plugin.tasks.length})
                                </span>
                            </p>
                            {plugin.deprecationMessage && (
                                <p
                                    style={{
                                        fontStyle: "italic",
                                        color: "var(--eccenca-color-greyscale-medium, #888)",
                                        margin: 0,
                                        fontSize: "0.875rem",
                                    }}
                                >
                                    {plugin.deprecationMessage}
                                </p>
                            )}
                            <p
                                style={{
                                    fontSize: "0.7rem",
                                    fontWeight: 600,
                                    letterSpacing: "0.08em",
                                    textTransform: "uppercase",
                                    color: "var(--eccencia-color-greyscale-medium, #888)",
                                    margin: "0.15rem 0 0",
                                }}
                            >
                                {t("widget.deprecatedPluginWidget.tasks")}
                            </p>
                            <ul
                                style={{
                                    display: "flex",
                                    flexWrap: "wrap",
                                    gap: "0.25rem 0.5rem",
                                    margin: 0,
                                    padding: 0,
                                    listStyle: "none",
                                }}
                            >
                                {plugin.tasks.map((task) => (
                                    <li
                                        key={task.link}
                                        style={{
                                            display: "flex",
                                            flexWrap: "wrap",
                                            alignItems: "center",
                                            gap: "0.5rem",
                                        }}
                                    >
                                        <Link
                                            onClick={(e) => {
                                                if (!e?.ctrlKey) {
                                                    e.preventDefault();
                                                    window.location.href = contextualPath(task.link);
                                                }
                                            }}
                                            href={task.link}
                                        >
                                            {task.label}
                                        </Link>
                                    </li>
                                ))}
                            </ul>
                        </div>
                    ))}
                </div>
                <Spacing />
                <Notification intent="warning">{t("widget.deprecatedPluginWidget.message")}</Notification>
            </CardContent>
        </Card>
    );
}
