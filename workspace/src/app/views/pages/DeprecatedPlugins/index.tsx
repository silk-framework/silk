import React, { useEffect, useMemo, useState } from "react";
import {
    Button,
    Card,
    ContextMenu,
    Divider,
    IconButton,
    Markdown,
    MenuItem,
    Notification,
    OverflowText,
    OverviewItem,
    OverviewItemActions,
    OverviewItemDepiction,
    OverviewItemDescription,
    OverviewItemLine,
    Spacing,
    Tag,
    TagList,
    TitleSubsection,
    Toolbar,
    ToolbarSection,
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
    deprecationMessage: string;
}

type GroupMode = "pluginLabel" | "project";

type GroupedSection = {
    key: string;
    items: DeprecatedPluginsModel[];
    deprecationMessage?: string;
};

// This page is used to display deprecated plugins that are still in use in existing projects. It serves as an overview for users to identify and replace deprecated plugins in their projects.
export default function DeprecatedPlugins() {
    const [deprecatedPlugins, setDeprecatedPlugins] = useState<DeprecatedPluginsModel[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [groupBy, setGroupBy] = useState<GroupMode>("pluginLabel");
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

    const groupedSections = useMemo((): GroupedSection[] => {
        const sorted = [...deprecatedPlugins].sort((a, b) => {
            const keyA = groupBy === "pluginLabel" ? (a.pluginLabel ?? "") : (a.projectLabel ?? "");
            const keyB = groupBy === "pluginLabel" ? (b.pluginLabel ?? "") : (b.projectLabel ?? "");
            const keyCmp = keyA.localeCompare(keyB);
            return keyCmp !== 0 ? keyCmp : (a.taskLabel ?? "").localeCompare(b.taskLabel ?? "");
        });

        const groups = new Map<string, DeprecatedPluginsModel[]>();
        sorted.forEach((plugin) => {
            const key = groupBy === "pluginLabel" ? (plugin.pluginLabel ?? "") : (plugin.projectLabel ?? "");
            if (!groups.has(key)) groups.set(key, []);
            groups.get(key)!.push(plugin);
        });

        return Array.from(groups.entries()).map(([key, items]) => ({
            key,
            items,
            deprecationMessage: groupBy === "pluginLabel" ? items[0]?.deprecationMessage : undefined,
        }));
    }, [deprecatedPlugins, groupBy]);

    const goToTaskPage = (link: string) => (e: React.MouseEvent) => {
        if (!e?.ctrlKey) {
            e.preventDefault();
            window.location.href = link;
        }
    };

    const currentGroupLabel =
        groupBy === "pluginLabel"
            ? t("pages.deprecatedPlugins.groupBy.pluginLabel")
            : t("pages.deprecatedPlugins.groupBy.project");

    const otherGroupLabel =
        groupBy === "pluginLabel"
            ? t("pages.deprecatedPlugins.groupBy.project")
            : t("pages.deprecatedPlugins.groupBy.pluginLabel");

    return (
        <div>
            {/* page header in app bar */}
            {pageHeader}
            {deprecatedPlugins.length > 0 && <Notification>{t("pages.deprecatedPlugins.infoMessage")}</Notification>}
            <Spacing size="small" />
            {deprecatedPlugins.length > 0 && (
                <Toolbar noWrap>
                    <ToolbarSection canGrow>
                        <TitleSubsection>
                            {`${t("pages.deprecatedPlugins.groupBy.groupedBy")}: ${currentGroupLabel}`}
                        </TitleSubsection>
                    </ToolbarSection>
                    <ToolbarSection>
                        <ContextMenu
                            togglerElement={
                                <Button
                                    text={`${t("pages.deprecatedPlugins.groupBy.label")}: ${currentGroupLabel}`}
                                    rightIcon="toggler-caretdown"
                                />
                            }
                        >
                            <MenuItem
                                text={t("pages.deprecatedPlugins.groupBy.pluginLabel")}
                                active={groupBy === "pluginLabel"}
                                icon={groupBy === "pluginLabel" ? "state-checked" : undefined}
                                onClick={() => setGroupBy("pluginLabel")}
                            />
                            <MenuItem
                                text={t("pages.deprecatedPlugins.groupBy.project")}
                                active={groupBy === "project"}
                                icon={groupBy === "project" ? "state-checked" : undefined}
                                onClick={() => setGroupBy("project")}
                            />
                        </ContextMenu>
                    </ToolbarSection>
                </Toolbar>
            )}
            <Spacing />
            <Datalist
                data-test-id="deprecated-plugins-list"
                isEmpty={!isLoading && deprecatedPlugins.length === 0}
                isLoading={isLoading}
                hasSpacing
                columns={1}
                emptyContainer={<Notification>{t("pages.deprecatedPlugins.noPluginsFound")}</Notification>}
            >
                {groupedSections.map(({ key, items, deprecationMessage }) => (
                    <React.Fragment key={key}>
                        {/* Group heading */}
                        <div style={{ paddingTop: "1rem", paddingBottom: "0.5rem" }}>
                            <TitleSubsection useHtmlElement="h3" style={{ margin: "0 0 0.25rem", display: "flex", alignItems: "center", gap: "0.5rem" }}>
                                {key}
                                <Tag emphasis="weak" style={{ fontWeight: "normal" }}>
                                    {items.length}
                                </Tag>
                            </TitleSubsection>
                            {deprecationMessage && (
                                <div style={{ marginBottom: "0.5rem", opacity: 0.75, fontSize: "0.875em" }}>
                                    {wrapTooltip(
                                        deprecationMessage.length > 120,
                                        <Markdown>{deprecationMessage}</Markdown>,
                                        <OverflowText passDown={true} inline={true}>
                                            {deprecationMessage.substring(0, 120)}
                                        </OverflowText>,
                                    )}
                                </div>
                            )}
                            <Divider />
                        </div>
                        {/* Items in this group */}
                        {items.map((plugin, index) => (
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
                                                        {plugin.taskLabel || t("pages.deprecatedPlugins.unknownTask")}
                                                    </OverflowText>
                                                </ResourceLink>
                                            </h4>
                                        </OverviewItemLine>
                                        {/* Line 2: context tags */}
                                        <OverviewItemLine small>
                                            <TagList>
                                                {groupBy === "pluginLabel" && plugin.projectLabel && (
                                                    <Tag emphasis="weak" itemType={plugin.itemType}>
                                                        {plugin.projectLabel}
                                                    </Tag>
                                                )}
                                                {groupBy === "project" && plugin.pluginLabel && (
                                                    <Tag emphasis="weak" intent="warning">
                                                        {plugin.pluginLabel}
                                                    </Tag>
                                                )}
                                                {plugin.itemType && (
                                                    <Tag emphasis="weak" itemType={plugin.itemType}>
                                                        {plugin.itemType}
                                                    </Tag>
                                                )}
                                            </TagList>
                                        </OverviewItemLine>
                                        {/* Line 3: deprecation message per item (group by project only) */}
                                        {groupBy === "project" && plugin.deprecationMessage && (
                                            <OverviewItemLine small>
                                                {wrapTooltip(
                                                    plugin.deprecationMessage.length > 80,
                                                    <Markdown>{plugin.deprecationMessage}</Markdown>,
                                                    <OverflowText passDown={true} inline={true}>
                                                        {plugin.deprecationMessage.substring(0, 80)}
                                                    </OverflowText>,
                                                )}
                                            </OverviewItemLine>
                                        )}
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
                    </React.Fragment>
                ))}
            </Datalist>
        </div>
    );
}
