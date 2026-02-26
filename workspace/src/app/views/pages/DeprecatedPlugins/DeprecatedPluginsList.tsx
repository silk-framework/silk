import React from "react";
import {
    Divider,
    IconButton,
    Markdown,
    Notification,
    OverflowText,
    OverviewItem,
    OverviewItemActions,
    OverviewItemDepiction,
    OverviewItemDescription,
    OverviewItemLine,
    Tag,
    TagList,
    TitleSubsection,
} from "@eccenca/gui-elements";
import { Datalist } from "../../shared/Datalist/Datalist";
import { ResourceLink } from "../../shared/ResourceLink/ResourceLink";
import { useTranslation } from "react-i18next";
import { contextualPath } from "../../../constants/path";
import { ItemDepiction } from "../../shared/ItemDepiction/ItemDepiction";
import { DeprecatedPluginsModel, PluginGroup } from "./index";

interface DeprecatedPluginsListProps {
    filteredPlugins: DeprecatedPluginsModel[];
    selectedPlugin: PluginGroup | null;
    selectedPluginKey: string | null;
    isLoading: boolean;
    hasCardWrapper?: boolean;
}

const goToTaskPage = (link: string) => (e: React.MouseEvent) => {
    if (!e?.ctrlKey) {
        e.preventDefault();
        window.location.href = link;
    }
};

export function DeprecatedPluginsList({ filteredPlugins, selectedPlugin, selectedPluginKey, isLoading, hasCardWrapper = true }: DeprecatedPluginsListProps) {
    const [t] = useTranslation();

    const renderItem = (plugin: DeprecatedPluginsModel) => (
        <OverviewItem
            key={`${plugin.project}_${plugin.task}_${plugin.pluginId}`}
            hasSpacing
            hasCardWrapper={hasCardWrapper}
            cardProps={hasCardWrapper ? { className: "diapp-searchitem" } : undefined}
            data-test-id="deprecated-plugin-item"
        >
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
    );

    // When no plugin filter is active, group items by plugin and render section headers
    if (!selectedPluginKey) {
        type GroupEntry = { pluginLabel: string; deprecationMessage: string; items: DeprecatedPluginsModel[] };
        const groups = filteredPlugins.reduce((acc, plugin) => {
            if (!acc[plugin.pluginId]) {
                acc[plugin.pluginId] = {
                    pluginLabel: plugin.pluginLabel,
                    deprecationMessage: plugin.deprecationMessage,
                    items: [],
                };
            }
            acc[plugin.pluginId].items.push(plugin);
            return acc;
        }, {} as Record<string, GroupEntry>);

        return (
            <>
                {!isLoading && filteredPlugins.length === 0 && (
                    <Notification>{t("pages.deprecatedPlugins.noPluginsFound")}</Notification>
                )}
                {Object.entries(groups).map(([pluginId, group], index) => (
                    <React.Fragment key={pluginId}>
                        {index > 0 && <Divider addSpacing="small" />}
                        <TitleSubsection useHtmlElement="h3">{group.pluginLabel}</TitleSubsection>
                        {group.deprecationMessage && (
                            <div style={{ marginBottom: "0.75rem", opacity: 0.75 }}>
                                <Markdown>{group.deprecationMessage}</Markdown>
                            </div>
                        )}
                        <Datalist
                            data-test-id="deprecated-plugins-list"
                            isLoading={isLoading}
                            hasSpacing
                            columns={1}
                        >
                            {group.items.map(renderItem)}
                        </Datalist>
                    </React.Fragment>
                ))}
            </>
        );
    }

    return (
        <>
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
                {filteredPlugins.map(renderItem)}
            </Datalist>
        </>
    );
}
