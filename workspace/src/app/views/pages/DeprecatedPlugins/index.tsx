import React, { useEffect, useState } from "react";
import {
    Card,
    IconButton,
    OverflowText,
    OverviewItem,
    OverviewItemActions,
    OverviewItemDepiction,
    OverviewItemDescription,
    OverviewItemLine,
    Tag,
    TagList,
    Notification,
    Spacing,
    Markdown,
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

// This page is used to display deprecated plugins that are still in use in existing projects. It serves as an overview for users to identify and replace deprecated plugins in their projects.
export default function DeprecatedPlugins() {
    const [deprecatedPlugins, setDeprecatedPlugins] = useState<DeprecatedPluginsModel[]>([]);
    const [isLoading, setIsLoading] = useState(true);
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
            {/* information alert to user when no deprecated plugins are found */}

            {deprecatedPlugins.length > 0 && <Notification>{t("pages.deprecatedPlugins.infoMessage")}</Notification>}
            <Spacing />
            {/* list of deprecated plugins in two column layout */}
            <Datalist
                data-test-id="deprecated-plugins-list"
                isEmpty={!isLoading && deprecatedPlugins.length === 0}
                isLoading={isLoading}
                hasSpacing
                columns={2}
                emptyContainer={<Notification>{t("pages.deprecatedPlugins.noPluginsFound")}</Notification>}
            >
                {deprecatedPlugins.map((plugin, index) => (
                    <Card key={`${plugin.project}_${plugin.task}_${index}`} isOnlyLayout className="diapp-searchitem">
                        <OverviewItem hasSpacing data-test-id="deprecated-plugin-item">
                            <OverviewItemDepiction>
                                <ItemDepiction itemType={plugin.itemType} pluginId={plugin.pluginLabel} />
                            </OverviewItemDepiction>
                            <OverviewItemDescription>
                                <OverviewItemLine>
                                    {/* task label */}
                                    <h4 style={{ display: "inline" }}>
                                        <ResourceLink
                                            url={plugin.taskLabel || false}
                                            handlerResourcePageLoader={plugin.link ? goToTaskPage(contextualPath(plugin.link)) : false}
                                        >
                                            <OverflowText>
                                                {plugin.taskLabel || t("pages.deprecatedPlugins.unknownTask")}
                                            </OverflowText>
                                        </ResourceLink>
                                    </h4>
                                    {/* deprecation message (cut by 80 characters) */}
                                    {plugin.deprecationMessage && (
                                        <span style={{ marginLeft: "0.2rem" }}>
                                            {wrapTooltip(
                                                plugin.deprecationMessage.length > 80,
                                                <Markdown>
                                                    {plugin.deprecationMessage ||
                                                        t("pages.deprecatedPlugins.deprecationMessage")}
                                                </Markdown>,
                                                <OverflowText passDown={true} inline={true}>
                                                    {plugin.deprecationMessage.substring(0, 80) ||
                                                        t("pages.deprecatedPlugins.deprecationMessage")}
                                                </OverflowText>,
                                            )}
                                        </span>
                                    )}
                                </OverviewItemLine>
                                <OverviewItemLine small>
                                    {/* Tags (Plugin label, project label, item label) */}
                                    <TagList>
                                        {plugin.pluginLabel && (
                                            <Tag emphasis="weak" itemType={plugin.itemType}>
                                                {plugin.pluginLabel}
                                            </Tag>
                                        )}
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
                                        href={contextualPath(plugin.link)}
                                    />
                                )}
                            </OverviewItemActions>
                        </OverviewItem>
                    </Card>
                ))}
            </Datalist>
        </div>
    );
}
