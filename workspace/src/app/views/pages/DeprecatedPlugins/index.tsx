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
import { usePageHeader } from "views/shared/PageHeader/PageHeader";

interface DeprecatedPluginsModel {
  project?: string;
  task?: string;
  link?: string;
  deprecationMessage?: string;
}

export default function DepricatedPlugins() {
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
  }, [registerError]);

  const goToTaskPage = (link: string) => (e: React.MouseEvent) => {
    // Only open page in same tab if user did not try to open in new tab
    if (!e?.ctrlKey) {
      e.preventDefault();
      window.location.href = link;
    }
  };

  return (
    <div>
      <h1>{t("pages.deprecatedPlugins.title", "Deprecated Plugins")}</h1>
      <Datalist
        data-test-id="deprecated-plugins-list"
        isEmpty={!isLoading && deprecatedPlugins.length === 0}
        isLoading={isLoading}
        hasSpacing
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
                  <h4>
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
                  {plugin.deprecationMessage && (
                    <>
                      {" "}
                      {wrapTooltip(
                        plugin.deprecationMessage.length > 80,
                        <OverflowText passDown={true} inline={true}>
                          {plugin.deprecationMessage}
                        </OverflowText>,
                        <div>{plugin.deprecationMessage}</div>,
                      )}
                    </>
                  )}
                </OverviewItemLine>
                <OverviewItemLine small>
                  <TagList>
                    {plugin.project && (
                      <Tag emphasis="weak">{plugin.project}</Tag>
                    )}
                  </TagList>
                </OverviewItemLine>
              </OverviewItemDescription>
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
