import React, { useEffect, useState } from "react";
import {
  Card,
  Icon,
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
} from "@eccenca/gui-elements";
import { Datalist } from "../../shared/Datalist/Datalist";
import { fetch } from "../../../services/fetch/fetch";
import { ResourceLink } from "../../shared/ResourceLink/ResourceLink";
import { wrapTooltip } from "../../../utils/uiUtils";
import { coreApi } from "../../../utils/getApiEndpoint";
import useErrorHandler from "../../../hooks/useErrorHandler";
import { useTranslation } from "react-i18next";
import { usePageHeader } from "../../../views/shared/PageHeader/PageHeader";
import { SERVE_PATH } from "../../../constants/path";

interface DeprecatedPluginsModel {
  project?: string;
  task?: string;
  link?: string;
  deprecationMessage?: string;
}

export default function DeprecatedPlugins() {
  const [deprecatedPlugins, setDeprecatedPlugins] = useState<
    DeprecatedPluginsModel[]
  >([]);
  const [isLoading, setIsLoading] = useState(true);
  const { registerError } = useErrorHandler();
  const [t] = useTranslation();

  const breadcrumbs = [
    {
      text: t("navigation.side.diBrowse"),
      href: SERVE_PATH,
    },
    {
      text: t("pages.deprecatedPlugins.title", "Deprecated Plugins"),
      current: true,
    },
  ];

  const { pageHeader } = usePageHeader({
    alternateDepiction: "state-warning",
    autogeneratePageTitle: true,
    breadcrumbs,
  });

  useEffect(() => {

    const fetchDeprecatedPlugins = async () => {
      try {
        const response = await fetch<DeprecatedPluginsModel[]>({
          url: coreApi("/usages/deprecatedPlugins"),
          method: "GET",
        });
        setDeprecatedPlugins(response.data);
      } catch (error) {
        registerError(
          "DeprecatedPlugins_FetchPlugins",
          t(
            "pages.deprecatedPlugins.errors.fetchPlugins",
            "Failed to load deprecated plugins",
          ),
          error,
        );
      } finally {
        setIsLoading(false);
      }
    };

    fetchDeprecatedPlugins();
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
      <Notification>
        {t(
          "pages.deprecatedPlugins.infoMessage",
          "This page lists plugins that are used in existing projects but have been marked as deprecated. It is recommended to replace these plugins to ensure compatibility with future versions of the application.",
        )}
      </Notification>
      <br/>
      {/* list of deprecated plugins in two column layout */}
      <Datalist
        data-test-id="deprecated-plugins-list"
        isEmpty={!isLoading && deprecatedPlugins.length === 0}
        isLoading={isLoading}
        hasSpacing
        columns={2}
        emptyContainer={
          <Notification>
            {t(
              "pages.deprecatedPlugins.noPluginsFound",
              "No deprecated plugins found.",
            )}
          </Notification>
        }
      >
        {deprecatedPlugins.map((plugin, index) => (
          <Card
            key={`${plugin.project}_${plugin.task}_${index}`}
            isOnlyLayout
            className="diapp-searchitem"
          >
            <OverviewItem hasSpacing data-test-id="deprecated-plugin-item">
              <OverviewItemDepiction>
                <Icon name="artefact-task" large />
              </OverviewItemDepiction>
              <OverviewItemDescription>
                <OverviewItemLine>
                  {/* task label */}
                  <h4 style={{ display: "inline" }}>
                    <ResourceLink
                      url={plugin.link || false}
                      handlerResourcePageLoader={
                        plugin.link ? goToTaskPage(plugin.link) : false
                      }
                    >

                      <OverflowText>
                        {plugin.task ||
                          t(
                            "pages.deprecatedPlugins.unknownTask",
                            "Unknown Task",
                          )}
                      </OverflowText>
                    </ResourceLink>
                  </h4>
                  {/* deprecation message (cut by 80 characters) */}
                  {plugin.deprecationMessage && (
                    <span style={{ marginLeft: "0.2rem" }}>
                      {wrapTooltip(
                        plugin.deprecationMessage.length > 80,
                        <OverflowText passDown={true} inline={true}>
                          {plugin.deprecationMessage ||
                          t(
                            "pages.deprecatedPlugins.deprecationMessage",
                            "Plugin is deprecated.",
                          )}
                        </OverflowText>,
                        <div>{plugin.deprecationMessage.substring(0, 80) ||
                          t(
                            "pages.deprecatedPlugins.deprecationMessage",
                            "Plugin is deprecated.",
                          )}</div>,
                      )}
                    </span>
                  )}
                </OverviewItemLine>
                <OverviewItemLine small>
                  {/* Tags (Plugin label, project label, item label) */}
                  <TagList>
                    {plugin.project && (
                      <Tag emphasis="weak">{plugin.project}</Tag>
                    )}
                  </TagList>
                </OverviewItemLine>
              </OverviewItemDescription>
              {/* interaction element to link to task page */}
              <OverviewItemActions>
                {plugin.link && (
                  <IconButton
                    name="item-viewdetails"
                    text={t("common.action.showDetails", "Show details")}
                    onClick={goToTaskPage(plugin.link)}
                    href={plugin.link}
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
