import React, { useEffect, useMemo, useState } from "react";
import {
    Grid,
    GridColumn,
    GridRow,
    Notification,
    Spacing,
    WorkspaceContent,
    WorkspaceMain,
    WorkspaceSide,
} from "@eccenca/gui-elements";
import useErrorHandler from "../../../hooks/useErrorHandler";
import { useTranslation } from "react-i18next";
import { usePageHeader } from "../../../views/shared/PageHeader/PageHeader";
import { SERVE_PATH } from "../../../constants/path";
import { requestDeprecatedPlugins } from "@ducks/common/requests";
import { DeprecatedPluginsSidebar } from "./DeprecatedPluginsSidebar";
import { DeprecatedPluginsList } from "./DeprecatedPluginsList";

export interface DeprecatedPluginsModel {
    project?: string;
    projectLabel?: string;
    task?: string;
    taskLabel?: string;
    itemType?: string;
    pluginId: string;
    pluginLabel: string;
    link?: string;
    linkLabel?: string;
    deprecationMessage?: string;
}

export type PluginGroup = {
    pluginId: string;
    pluginLabel: string;
    count: number;
    deprecationMessage: string;
};

// This page is used to display deprecated plugins that are still in use in existing projects. It serves as an overview for users to identify and replace deprecated plugins in their projects.
export default function DeprecatedPlugins() {
    const [deprecatedPlugins, setDeprecatedPlugins] = useState<DeprecatedPluginsModel[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [selectedPluginKey, setSelectedPluginKey] = useState<string | null>(null);
    const { registerError } = useErrorHandler();
    const [t] = useTranslation();

    const breadcrumbs = [
        {
            text: t("navigation.side.diBrowse"),
            href: SERVE_PATH,
        },
        {
            text: t("pages.deprecatedPlugins.title"),
            current: true,
        },
    ];

    const { pageHeader } = usePageHeader({
        alternateDepiction: "state-warning",
        autogeneratePageTitle: true,
        breadcrumbs,
    });

    useEffect(() => {
        requestDeprecatedPlugins()
            .then((response) => {
                setDeprecatedPlugins(response.data);
            })
            .catch((error) => {
                registerError(
                    "DeprecatedPlugins_FetchPlugins",
                    t("pages.deprecatedPlugins.errors.fetchPlugins"),
                    error,
                );
            })
            .finally(() => {
                setIsLoading(false);
            });
    }, []);

    const sortedPlugins = useMemo((): DeprecatedPluginsModel[] => {
        return [...deprecatedPlugins].sort((a, b) => {
            const keyCmp = (a.pluginLabel ?? "").localeCompare(b.pluginLabel ?? "");
            return keyCmp !== 0 ? keyCmp : (a.taskLabel ?? "").localeCompare(b.taskLabel ?? "");
        });
    }, [deprecatedPlugins]);

    const pluginGroups = useMemo((): PluginGroup[] => {
        const groups = new Map<string, PluginGroup>();
        sortedPlugins.forEach((plugin) => {
            const id = plugin.pluginId;
            if (!groups.has(id)) {
                groups.set(id, {
                    pluginId: id,
                    pluginLabel: plugin.pluginLabel ?? "",
                    count: 0,
                    deprecationMessage: plugin.deprecationMessage ?? "",
                });
            }
            groups.get(id)!.count++;
        });
        return Array.from(groups.values());
    }, [sortedPlugins]);

    useEffect(() => {
        if (pluginGroups.length === 0) return;
        const isValidSelection = pluginGroups.some((g) => g.pluginId === selectedPluginKey);
        if (!isValidSelection) {
            setSelectedPluginKey(pluginGroups[0].pluginId);
        }
    }, [pluginGroups]);

    const selectedPlugin = useMemo(
        () => pluginGroups.find((g) => g.pluginId === selectedPluginKey) ?? null,
        [pluginGroups, selectedPluginKey],
    );

    const filteredPlugins = useMemo(
        () => (selectedPluginKey ? sortedPlugins.filter((p) => p.pluginId === selectedPluginKey) : sortedPlugins),
        [sortedPlugins, selectedPluginKey],
    );

    return (
        <WorkspaceContent>
            {pageHeader /* page header in app bar */}
            {!isLoading && deprecatedPlugins.length === 0 && (
                <Notification>{t("pages.deprecatedPlugins.noPluginsFound")}</Notification>
            )}
            {deprecatedPlugins.length > 0 && <Notification>{t("pages.deprecatedPlugins.infoMessage")}</Notification>}
            <Spacing />
            <WorkspaceMain>
                <Grid>
                    <GridRow>
                        {/* Left sidebar: plugin filter */}
                        <GridColumn small>
                            <DeprecatedPluginsSidebar
                                pluginGroups={pluginGroups}
                                selectedPluginKey={selectedPluginKey}
                                onSelectPlugin={setSelectedPluginKey}
                            />
                        </GridColumn>
                        {/* Right content: heading + task list */}
                        <GridColumn>
                            <DeprecatedPluginsList
                                filteredPlugins={filteredPlugins}
                                selectedPlugin={selectedPlugin}
                                selectedPluginKey={selectedPluginKey}
                                isLoading={isLoading}
                            />
                        </GridColumn>
                    </GridRow>
                </Grid>
            </WorkspaceMain>
            <WorkspaceSide></WorkspaceSide>
        </WorkspaceContent>
    );
}
