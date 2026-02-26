import React, { useEffect, useMemo, useState } from "react";
import {
    Card,
    Divider,
    Grid,
    GridColumn,
    GridRow,
    IconButton,
    Label,
    Markdown,
    Notification,
    OverflowText,
    OverviewItem,
    OverviewItemActions,
    OverviewItemDepiction,
    OverviewItemDescription,
    OverviewItemLine,
    RadioButton,
    Spacing,
    Tag,
    TagList,
    TitleSubsection,
} from "@eccenca/gui-elements";
import { Datalist } from "../../shared/Datalist/Datalist";
import { ResourceLink } from "../../shared/ResourceLink/ResourceLink";
import { wrapTooltip } from "../../../utils/uiUtils";
import useErrorHandler from "../../../hooks/useErrorHandler";
import { useTranslation } from "react-i18next";
import { usePageHeader } from "../../../views/shared/PageHeader/PageHeader";
import { SERVE_PATH, contextualPath } from "../../../constants/path";
import { ItemDepiction } from "../../shared/ItemDepiction/ItemDepiction";
import { requestDeprecatedPlugins } from "@ducks/common/requests";

export interface DeprecatedPluginsModel {
    project: string;
    projectLabel: string;
    task: string;
    taskLabel: string;
    itemType: string;
    pluginId: string;
    pluginLabel: string;
    link: string;
    linkLabel: string;
    deprecationMessage: string;
}

type PluginGroup = {
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
        () =>
            selectedPluginKey
                ? sortedPlugins.filter((p) => p.pluginId === selectedPluginKey)
                : sortedPlugins,
        [sortedPlugins, selectedPluginKey],
    );

    const goToTaskPage = (link: string) => (e: React.MouseEvent) => {
        if (!e?.ctrlKey) {
            e.preventDefault();
            window.location.href = link;
        }
    };

    return (
        <div>
            {/* page header in app bar */}
            {pageHeader}
            {deprecatedPlugins.length > 0 && (
                <Notification>{t("pages.deprecatedPlugins.infoMessage")}</Notification>
            )}
            <Spacing />
            <Grid>
                <GridRow>
                    {/* Left sidebar: plugin filter */}
                    <GridColumn small>
                        <nav>
                            <TitleSubsection>
                                <Label
                                    isLayoutForElement="h3"
                                    text={t("pages.deprecatedPlugins.title")}
                                />
                            </TitleSubsection>
                            <Spacing size="tiny" />
                            <ul>
                                {pluginGroups.map(({ pluginId, pluginLabel, count }) => (
                                    <li key={pluginId}>
                                        <RadioButton
                                            checked={selectedPluginKey === pluginId}
                                            label={`${pluginLabel} (${count})`}
                                            onChange={() => setSelectedPluginKey(pluginId)}
                                            value={pluginId}
                                        />
                                    </li>
                                ))}
                            </ul>
                        </nav>
                    </GridColumn>
                    {/* Right content: heading + task list */}
                    <GridColumn>
                        {selectedPlugin && (
                            <>
                                <TitleSubsection useHtmlElement="h3">{selectedPlugin.pluginLabel}</TitleSubsection>
                                {selectedPlugin.deprecationMessage && (
                                    <div style={{ marginBottom: "0.75rem", opacity: 0.75 }}>
                                        <Markdown>{selectedPlugin.deprecationMessage}</Markdown>
                                    </div>
                                )}
                                <Divider addSpacing="small" />
                            </>
                        )}
                        <Datalist
                            data-test-id="deprecated-plugins-list"
                            isEmpty={!isLoading && filteredPlugins.length === 0}
                            isLoading={isLoading}
                            hasSpacing
                            columns={1}
                            emptyContainer={
                                <Notification>{t("pages.deprecatedPlugins.noPluginsFound")}</Notification>
                            }
                        >
                            {filteredPlugins.map((plugin) => (
                                <Card
                                    key={`${plugin.project}_${plugin.task}_${plugin.pluginId}`}
                                    isOnlyLayout
                                    className="diapp-searchitem"
                                >
                                    <OverviewItem hasSpacing data-test-id="deprecated-plugin-item">
                                        <OverviewItemDepiction>
                                            <ItemDepiction itemType={plugin.itemType} pluginId={plugin.pluginId} />
                                        </OverviewItemDepiction>
                                        <OverviewItemDescription>
                                            {/* Line 1: task name */}
                                            <OverviewItemLine>
                                                <h4 style={{ margin: 0 }}>
                                                    <ResourceLink
                                                        url={plugin.link || false}
                                                        handlerResourcePageLoader={
                                                            plugin.link
                                                                ? goToTaskPage(contextualPath(plugin.link))
                                                                : false
                                                        }
                                                    >
                                                        <OverflowText>
                                                            {plugin.taskLabel ||
                                                                t("pages.deprecatedPlugins.unknownTask")}
                                                        </OverflowText>
                                                    </ResourceLink>
                                                </h4>
                                            </OverviewItemLine>
                                            {/* Line 2: context tags */}
                                            <OverviewItemLine small>
                                                <TagList>
                                                    {plugin.projectLabel && (
                                                        <Tag emphasis="weak" itemType={plugin.itemType}>
                                                            {plugin.projectLabel}
                                                        </Tag>
                                                    )}
                                                    {plugin.itemType && (
                                                        <Tag emphasis="weak" itemType={plugin.itemType}>
                                                            {plugin.itemType}
                                                        </Tag>
                                                    )}
                                                    {/* Only show plugin tag when not filtered to a single plugin */}
                                                    {!selectedPluginKey &&
                                                        plugin.pluginLabel &&
                                                        wrapTooltip(
                                                            !!plugin.deprecationMessage,
                                                            <Markdown>{plugin.deprecationMessage}</Markdown>,
                                                            <Tag emphasis="weak">{plugin.pluginLabel}</Tag>,
                                                        )}
                                                </TagList>
                                            </OverviewItemLine>
                                        </OverviewItemDescription>
                                        {/* interaction element to link to task page */}
                                        <OverviewItemActions>
                                            {plugin.link && (
                                                <IconButton
                                                    name="item-viewdetails"
                                                    text={t("common.action.showDetails")}
                                                    onClick={goToTaskPage(contextualPath(plugin.link))}
                                                    href={plugin.link}
                                                />
                                            )}
                                        </OverviewItemActions>
                                    </OverviewItem>
                                </Card>
                            ))}
                        </Datalist>
                    </GridColumn>
                </GridRow>
            </Grid>
        </div>
    );
}
