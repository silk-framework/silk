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
    key: string;
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

        return () => {};
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
            const key = plugin.pluginLabel ?? "";
            if (!groups.has(key)) {
                groups.set(key, { key, count: 0, deprecationMessage: plugin.deprecationMessage ?? "" });
            }
            groups.get(key)!.count++;
        });
        return Array.from(groups.values());
    }, [sortedPlugins]);

    const selectedPlugin = useMemo(
        () => pluginGroups.find((g) => g.key === selectedPluginKey) ?? null,
        [pluginGroups, selectedPluginKey],
    );

    const filteredPlugins = useMemo(
        () =>
            selectedPluginKey
                ? sortedPlugins.filter((p) => p.pluginLabel === selectedPluginKey)
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
                                <li>
                                    <RadioButton
                                        checked={selectedPluginKey === null}
                                        label={t("common.messages.allTypes", "All")}
                                        onChange={() => setSelectedPluginKey(null)}
                                        value=""
                                    />
                                </li>
                                {pluginGroups.map(({ key, count }) => (
                                    <li key={key}>
                                        <RadioButton
                                            checked={selectedPluginKey === key}
                                            label={`${key} (${count})`}
                                            onChange={() => setSelectedPluginKey(key)}
                                            value={key}
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
                                <TitleSubsection useHtmlElement="h3">{selectedPlugin.key}</TitleSubsection>
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
                            {filteredPlugins.map((plugin, index) => (
                                <Card
                                    key={`${plugin.project}_${plugin.task}_${index}`}
                                    isOnlyLayout
                                    className="diapp-searchitem"
                                    style={{ marginBottom: "0.375rem" }}
                                >
                                    <OverviewItem hasSpacing data-test-id="deprecated-plugin-item">
                                        <OverviewItemDepiction>
                                            <ItemDepiction itemType={plugin.itemType} pluginId={plugin.pluginLabel} />
                                        </OverviewItemDepiction>
                                        <OverviewItemDescription>
                                            {/* Line 1: task name */}
                                            <OverviewItemLine>
                                                <h4 style={{ margin: 0 }}>
                                                    <ResourceLink
                                                        url={plugin.taskLabel || false}
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
