import { Card, CardContent, CardHeader, CardTitle, Divider, Link, Notification, Spacing } from "@eccenca/gui-elements";
import useErrorHandler from "../../../../hooks/useErrorHandler";
import { contextualPath } from "../../../../constants/path";
import React, { useState, useEffect } from "react";
import { useTranslation } from "react-i18next";
import { DeprecatedPluginsModel } from "../../DeprecatedPlugins";
import { requestDeprecatedPlugins } from "@ducks/common/requests";
import styles from "./index.module.scss";

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
                <div className={styles.pluginList}>
                    {groupByPluginWithLinks(deprecatedPlugins).map((plugin) => (
                        <div key={plugin.pluginLabel} className={styles.pluginEntry}>
                            <p className={styles.pluginName}>
                                {plugin.pluginLabel}
                                <span className={styles.pluginCount}>({plugin.tasks.length})</span>
                            </p>
                            {plugin.deprecationMessage && (
                                <p className={styles.deprecationMessage}>{plugin.deprecationMessage}</p>
                            )}
                            <p className={styles.tasksLabel}>{t("widget.deprecatedPluginWidget.tasks")}</p>
                            <ul className={styles.taskList}>
                                {plugin.tasks.map((task) => (
                                    <li key={task.link} className={styles.taskItem}>
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
