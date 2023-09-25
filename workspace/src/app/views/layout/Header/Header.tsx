import React, { useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { useTranslation } from "react-i18next";
import { useLocation } from "react-router-dom";
import {
    ApplicationHeader,
    ApplicationSidebarNavigation,
    ApplicationSidebarToggler,
    ApplicationTitle,
    ApplicationToolbar,
    ApplicationToolbarAction,
    ApplicationToolbarPanel,
    ApplicationToolbarSection,
    Divider,
    HtmlContentBlock,
    Icon,
    Menu,
    MenuDivider,
    MenuItem,
    TitleSubsection,
    Toolbar,
    ToolbarSection,
    WorkspaceHeader,
    Tag,
} from "@eccenca/gui-elements";
import { commonOp, commonSel } from "@ducks/common";
import { routerOp } from "@ducks/router";
import CreateButton from "../../shared/buttons/CreateButton";
import { CreateArtefactModal } from "../../shared/modals/CreateArtefactModal/CreateArtefactModal";
import { NotificationsMenu } from "../../shared/ApplicationNotifications/NotificationsMenu";
import { triggerHotkeyHandler } from "../../shared/HotKeyHandler/HotKeyHandler";
import { APPLICATION_CORPORATION_NAME, APPLICATION_NAME, APPLICATION_SUITE_NAME } from "../../../constants/base";
import { CONTEXT_PATH, SERVE_PATH } from "../../../constants/path";
import { APP_VIEWHEADER_ID } from "../../shared/PageHeader/PageHeader";
import { pluginRegistry, SUPPORTED_PLUGINS } from "../../plugins/PluginRegistry";
import { UserMenuFooterProps } from "../../plugins/plugin.types";
import { ExampleProjectImportMenu } from "./ExampleProjectImportMenu";
import { useKeyboardHeaderShortcuts } from "./useKeyBoardHeaderShortcuts";

interface IProps {
    onClickApplicationSidebarExpand: any;
    isApplicationSidebarExpanded: boolean;
}

export function Header({ onClickApplicationSidebarExpand, isApplicationSidebarExpanded }: IProps) {
    const dispatch = useDispatch();
    const location = useLocation();
    const locationParams = new URLSearchParams(location.search?.substring(1));
    const { hotKeys } = useSelector(commonSel.initialSettingsSelector);
    const { dmBaseUrl, dmModuleLinks, version } = useSelector(commonSel.initialSettingsSelector);
    const [t] = useTranslation();
    const [displayUserMenu, toggleUserMenuDisplay] = useState<boolean>(false);
    //general keyboard shortcuts
    useKeyboardHeaderShortcuts();
    const diUserMenuItems = pluginRegistry.pluginReactComponent<{}>(SUPPORTED_PLUGINS.DI_USER_MENU_ITEMS);
    const diUserMenuFooter = pluginRegistry.pluginReactComponent<UserMenuFooterProps>(
        SUPPORTED_PLUGINS.DI_USER_MENU_FOOTER
    );
    const languageSwitcher = pluginRegistry.pluginReactComponent<{}>(SUPPORTED_PLUGINS.DI_LANGUAGE_SWITCHER);

    const handleCreateDialog = React.useCallback(() => {
        dispatch(commonOp.setSelectedArtefactDType("all"));
    }, []);

    const handleNavigate = (path: string) => {
        dispatch(routerOp.goToPage(path));
    };

    const searchURL = (page: string) => `?itemType=${page}&page=1&limit=10`;
    const brandingSuffix =
        APPLICATION_CORPORATION_NAME() || APPLICATION_SUITE_NAME()
            ? ` @ ${APPLICATION_CORPORATION_NAME()} ${APPLICATION_SUITE_NAME()}`
            : "";

    const activitiesPageLink = SERVE_PATH + "/activities";
    const activitiesPageQueries = "?page=1&limit=25&sortBy=recentlyUpdated&sortOrder=ASC";

    return (
        <>
            <ApplicationHeader aria-label={`${APPLICATION_NAME()}${brandingSuffix}`}>
                <ApplicationTitle
                    href={SERVE_PATH}
                    prefix={""}
                    isNotDisplayed={!isApplicationSidebarExpanded}
                    isApplicationSidebarExpanded={isApplicationSidebarExpanded}
                    depiction={
                        <img
                            src={CONTEXT_PATH + "/core/logoSmall.png"}
                            alt={`Logo: ${APPLICATION_NAME()}${brandingSuffix}`}
                        />
                    }
                >
                    {APPLICATION_NAME()}
                </ApplicationTitle>
                <ApplicationSidebarToggler
                    aria-label={
                        isApplicationSidebarExpanded
                            ? t("navigation.side.close", "Close navigation")
                            : t("navigation.side.open", "Open navigation")
                    }
                    onClick={onClickApplicationSidebarExpand}
                    isActive={isApplicationSidebarExpanded}
                />
                <ApplicationSidebarNavigation
                    isRail={!isApplicationSidebarExpanded}
                    expanded={isApplicationSidebarExpanded}
                >
                    {!!dmBaseUrl ? (
                        <>
                            <TitleSubsection title={t("navigation.side.dmBrowserTooltip", "")}>
                                {t("navigation.side.dmBrowser", "Explore")}
                            </TitleSubsection>
                            <Menu>
                                {dmModuleLinks ? (
                                    dmModuleLinks.map((link) => (
                                        <MenuItem
                                            icon={link.icon ? [link.icon] : undefined}
                                            text={t("navigation.side.dm." + link.path, link.defaultLabel)}
                                            htmlTitle={t("navigation.side.dm." + link.path + "Tooltip")}
                                            href={dmBaseUrl + "/" + link.path}
                                            key={link.path}
                                        />
                                    ))
                                ) : (
                                    <MenuItem
                                        icon="application-explore"
                                        text={t("navigation.side.dm.explore", "Knowledge Graphs")}
                                        htmlTitle={t("navigation.side.dm.exploreTooltip")}
                                        href={dmBaseUrl}
                                    />
                                )}
                            </Menu>
                            <Divider addSpacing="xlarge" />
                        </>
                    ) : (
                        <></>
                    )}
                    <TitleSubsection title={t("navigation.side.diBrowseTooltip", "")}>
                        {t("navigation.side.diBrowse", "")}
                    </TitleSubsection>
                    <Menu>
                        <MenuItem
                            icon="artefact-project"
                            text={t("navigation.side.di.projects", "Projects")}
                            htmlTitle={t("navigation.side.di.projectsTooltip")}
                            onClick={(e) => {
                                e.preventDefault();
                                e.stopPropagation();
                                handleNavigate(SERVE_PATH + searchURL("project"));
                            }}
                            href={SERVE_PATH + searchURL("project")}
                            active={location.pathname === SERVE_PATH && locationParams.get("itemType") === "project"}
                        />
                        <MenuItem
                            icon="artefact-dataset"
                            text={t("navigation.side.di.datasets", "Datasets")}
                            htmlTitle={t("navigation.side.di.datasetsTooltip")}
                            onClick={(e) => {
                                e.preventDefault();
                                e.stopPropagation();
                                handleNavigate(SERVE_PATH + searchURL("dataset"));
                            }}
                            href={SERVE_PATH + searchURL("dataset")}
                            active={location.pathname === SERVE_PATH && locationParams.get("itemType") === "dataset"}
                        />
                        <MenuItem
                            icon="artefact-workflow"
                            text={t("navigation.side.di.workflows", "Workflows")}
                            htmlTitle={t("navigation.side.di.workflowsTooltip")}
                            onClick={(e) => {
                                e.preventDefault();
                                e.stopPropagation();
                                handleNavigate(SERVE_PATH + searchURL("workflow"));
                            }}
                            href={SERVE_PATH + searchURL("workflow")}
                            active={location.pathname === SERVE_PATH && locationParams.get("itemType") === "workflow"}
                        />
                        <MenuItem
                            icon="application-activities"
                            text={t("navigation.side.di.activities", "Activities")}
                            htmlTitle={t("navigation.side.di.activitiesTooltip")}
                            onClick={(e) => {
                                e.preventDefault();
                                e.stopPropagation();
                                handleNavigate(activitiesPageLink + activitiesPageQueries);
                            }}
                            href={activitiesPageLink}
                            active={location.pathname.includes(activitiesPageLink)}
                        />
                    </Menu>
                </ApplicationSidebarNavigation>

                <WorkspaceHeader id={APP_VIEWHEADER_ID} />

                <ApplicationToolbar>
                    <ApplicationToolbarSection>
                        <CreateButton onClick={handleCreateDialog} />
                    </ApplicationToolbarSection>
                    <NotificationsMenu />
                    {displayUserMenu ? (
                        <>
                            <ApplicationToolbarAction
                                aria-label={t("navigation.user.close", "Close user menu")}
                                tooltipAlignment="end"
                                isActive={true}
                                onClick={() => {
                                    toggleUserMenuDisplay(false);
                                }}
                            >
                                <Icon name="navigation-close" description="Close icon" large />
                            </ApplicationToolbarAction>
                            <ApplicationToolbarPanel
                                aria-label="User menu"
                                expanded={true}
                                onLeave={() => {
                                    toggleUserMenuDisplay(false);
                                }}
                            >
                                <Toolbar verticalStack={true} style={{ height: "100%" }}>
                                    <ToolbarSection canGrow={true} style={{ width: "100%" }}>
                                        <Menu>
                                            {languageSwitcher && <languageSwitcher.Component />}
                                            <MenuDivider />
                                            {hotKeys.quickSearch && (
                                                <MenuItem
                                                    text={t("RecentlyViewedModal.title")}
                                                    href={"#"}
                                                    onClick={(e) => {
                                                        if (e) {
                                                            e.preventDefault();
                                                        }
                                                        triggerHotkeyHandler(hotKeys.quickSearch as string);
                                                    }}
                                                    icon={"operation-search"}
                                                    labelElement={
                                                        <Tag
                                                            htmlTitle={`Hotkey: ${hotKeys.quickSearch}`}
                                                            emphasis="weaker"
                                                        >
                                                            {hotKeys.quickSearch}
                                                        </Tag>
                                                    }
                                                />
                                            )}
                                            {hotKeys.overview && (
                                                <MenuItem
                                                    text={t("header.keyboardShortcutsModal.title")}
                                                    href={"#"}
                                                    onClick={(e) => {
                                                        if (e) {
                                                            e.preventDefault();
                                                        }
                                                        triggerHotkeyHandler(hotKeys.overview as string);
                                                    }}
                                                    icon="application-hotkeys"
                                                    labelElement={
                                                        <Tag
                                                            htmlTitle={`Hotkey: ${hotKeys.overview}`}
                                                            emphasis="weaker"
                                                        >
                                                            {hotKeys.overview}
                                                        </Tag>
                                                    }
                                                />
                                            )}
                                            <MenuItem
                                                text={t("common.action.showApiDoc", "API")}
                                                href={CONTEXT_PATH + "/doc/api"}
                                                icon={"application-homepage"}
                                            />
                                            <ExampleProjectImportMenu />
                                            {!!dmBaseUrl && diUserMenuItems && <diUserMenuItems.Component />}
                                        </Menu>
                                    </ToolbarSection>
                                    {diUserMenuFooter ? (
                                        <diUserMenuFooter.Component version={version} />
                                    ) : (
                                        version && (
                                            <ToolbarSection style={{ width: "10%" }}>
                                                <HtmlContentBlock small>{version}</HtmlContentBlock>
                                            </ToolbarSection>
                                        )
                                    )}
                                </Toolbar>
                            </ApplicationToolbarPanel>
                        </>
                    ) : (
                        <ApplicationToolbarAction
                            id={"headerUserMenu"}
                            aria-label={t("navigation.user.open", "Open user menu")}
                            tooltipAlignment="end"
                            isActive={false}
                            onClick={() => {
                                toggleUserMenuDisplay(true);
                            }}
                        >
                            <Icon name="application-useraccount" description="User menu icon" large />
                        </ApplicationToolbarAction>
                    )}
                </ApplicationToolbar>
                <CreateArtefactModal />
            </ApplicationHeader>
        </>
    );
}
