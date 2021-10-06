import React, { useState } from "react";
import Store from "store";
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
    Button,
    Divider,
    Icon,
    Menu,
    MenuDivider,
    MenuItem,
    TitleSubsection,
    Spacing,
    WorkspaceHeader,
} from "@gui-elements/index";
import { commonOp, commonSel } from "@ducks/common";
import { workspaceOp, workspaceSel } from "@ducks/workspace";
import { routerOp } from "@ducks/router";
import CreateButton from "../../shared/buttons/CreateButton";
import { CreateArtefactModal } from "../../shared/modals/CreateArtefactModal/CreateArtefactModal";
import { NotificationsMenu } from "../../shared/ApplicationNotifications/NotificationsMenu";
import { triggerHotkeyHandler } from "../../shared/HotKeyHandler/HotKeyHandler";
import { APPLICATION_CORPORATION_NAME, APPLICATION_NAME, APPLICATION_SUITE_NAME } from "../../../constants/base";
import { CONTEXT_PATH, SERVE_PATH } from "../../../constants/path";
import { APP_VIEWHEADER_ID } from "../../shared/PageHeader/PageHeader";

interface IProps {
    onClickApplicationSidebarExpand: any;
    isApplicationSidebarExpanded: boolean;
}

export function Header({ onClickApplicationSidebarExpand, isApplicationSidebarExpanded }: IProps) {
    const dispatch = useDispatch();
    const brwsrLocation = useLocation();
    const brwsrParams = new URLSearchParams(brwsrLocation.search?.substring(1));
    const [currentLanguage, setCurrentLanguage] = useState(Store.get("locale"));
    const { hotKeys } = useSelector(commonSel.initialSettingsSelector);
    const isAuth = useSelector(commonSel.isAuthSelector);
    const { dmBaseUrl, dmModuleLinks } = useSelector(commonSel.initialSettingsSelector);
    const appliedFilters = useSelector(workspaceSel.appliedFiltersSelector);
    const [t] = useTranslation();
    const [displayUserMenu, toggleUserMenuDisplay] = useState<boolean>(false);

    const handleCreateDialog = () => {
        dispatch(commonOp.setSelectedArtefactDType(appliedFilters.itemType));
    };

    const handleLanguageChange = (locale: string) => {
        setCurrentLanguage(locale);
        dispatch(commonOp.changeLocale(locale));
    };

    const handleNavigate = (page: string) => {
        dispatch(routerOp.goToPage(""));
        dispatch(
            workspaceOp.applyFiltersOp({
                itemType: page,
            })
        );
    };

    return !isAuth ? (
        <></>
    ) : (
        <ApplicationHeader
            aria-label={`${APPLICATION_NAME} @ ${APPLICATION_CORPORATION_NAME} ${APPLICATION_SUITE_NAME}`}
        >
            <ApplicationTitle
                href={SERVE_PATH}
                prefix={""}
                isNotDisplayed={!isApplicationSidebarExpanded}
                isApplicationSidebarExpanded={isApplicationSidebarExpanded}
                depiction={
                    <img
                        src={CONTEXT_PATH + "/core/logoSmall.png"}
                        alt={`Logo: ${APPLICATION_NAME} @ ${APPLICATION_CORPORATION_NAME} ${APPLICATION_SUITE_NAME}`}
                    />
                }
            >
                {APPLICATION_NAME}
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
                                        icon={link.icon}
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
                    {t("navigation.side.diBrowse", "Build")}
                </TitleSubsection>
                <Menu>
                    <MenuItem
                        icon="artefact-project"
                        text={t("navigation.side.di.projects", "Projects")}
                        htmlTitle={t("navigation.side.di.projectsTooltip")}
                        onClick={() => handleNavigate("project")}
                        active={brwsrLocation.pathname === SERVE_PATH && brwsrParams.get("itemType") === "project"}
                    />
                    <MenuItem
                        icon="artefact-dataset"
                        text={t("navigation.side.di.datasets", "Datasets")}
                        htmlTitle={t("navigation.side.di.datasetsTooltip")}
                        onClick={() => handleNavigate("dataset")}
                        active={brwsrLocation.pathname === SERVE_PATH && brwsrParams.get("itemType") === "dataset"}
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
                            aria-label="Close user menu"
                            isActive={true}
                            onClick={() => {
                                toggleUserMenuDisplay(false);
                            }}
                        >
                            <Icon name="navigation-close" description="Close icon" large />
                        </ApplicationToolbarAction>
                        <ApplicationToolbarPanel aria-label="User menu" expanded={true}>
                            <Menu>
                                <div>
                                    <Button
                                        onClick={() => handleLanguageChange("en")}
                                        disabled={currentLanguage === "en"}
                                    >
                                        En
                                    </Button>
                                    <Button
                                        onClick={() => handleLanguageChange("de")}
                                        disabled={currentLanguage === "de"}
                                    >
                                        De
                                    </Button>
                                </div>
                                <MenuDivider />
                                {hotKeys.quickSearch && (
                                    <MenuItem
                                        text={
                                            <>
                                                {t("RecentlyViewedModal.title")}
                                                <Spacing vertical={true} size="small" />
                                                <Button
                                                    outlined={true}
                                                    small={true}
                                                    tooltip={`Hotkey: ${hotKeys.quickSearch}`}
                                                >
                                                    {hotKeys.quickSearch}
                                                </Button>
                                            </>
                                        }
                                        href={"#"}
                                        onClick={(e) => {
                                            if (e) {
                                                e.preventDefault();
                                            }
                                            triggerHotkeyHandler(hotKeys.quickSearch as string);
                                        }}
                                        icon={"operation-search"}
                                    />
                                )}
                                <MenuItem
                                    text={t("common.action.activity", "Activity overview")}
                                    href={CONTEXT_PATH + "/workspace/allActivities"}
                                    icon={"application-activities"}
                                />
                                <MenuItem
                                    text={t("common.action.showApiDoc", "API")}
                                    href={CONTEXT_PATH + "/doc/api"}
                                    icon={"application-homepage"}
                                />
                                <MenuItem
                                    text={t("common.action.backOld", "Back to old workspace")}
                                    href={CONTEXT_PATH + "/workspace"}
                                    icon={"application-legacygui"}
                                />
                                {!!dmBaseUrl && (
                                    <>
                                        <MenuDivider />
                                        <MenuItem
                                            id={"logoutAction"}
                                            text={t("common.action.logout", "Logout")}
                                            onClick={() => {
                                                dispatch(commonOp.logoutFromDi());
                                            }}
                                            icon={"operation-logout"}
                                        />
                                    </>
                                )}
                            </Menu>
                        </ApplicationToolbarPanel>
                    </>
                ) : (
                    <ApplicationToolbarAction
                        id={"headerUserMenu"}
                        aria-label="Open user menu"
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
    );
}
