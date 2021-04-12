import React, { useState } from "react";
import Store from "store";
import { useDispatch, useSelector } from "react-redux";
import { useTranslation } from "react-i18next";
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
import { triggerHotkeyHandler } from "../../shared/HotKeyHandler/HotKeyHandler";
import { APPLICATION_CORPORATION_NAME, APPLICATION_NAME, APPLICATION_SUITE_NAME } from "../../../constants/base";
import { CONTEXT_PATH } from "../../../constants/path";
import { APP_VIEWHEADER_ID } from "../../shared/PageHeader/PageHeader";

interface IProps {
    onClickApplicationSidebarExpand: any;
    isApplicationSidebarExpanded: boolean;
}

export function Header({ onClickApplicationSidebarExpand, isApplicationSidebarExpanded }: IProps) {
    const dispatch = useDispatch();

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

    return !isAuth ? null : (
        <ApplicationHeader
            aria-label={`${APPLICATION_CORPORATION_NAME} ${APPLICATION_SUITE_NAME}: ${APPLICATION_NAME}`}
        >
            <ApplicationTitle
                prefix={APPLICATION_CORPORATION_NAME}
                isNotDisplayed={!isApplicationSidebarExpanded}
                isAlignedWithSidebar={isApplicationSidebarExpanded}
            >
                {APPLICATION_NAME}
            </ApplicationTitle>
            <ApplicationSidebarToggler
                aria-label={t("navigation.side.open", "Open navigation")}
                onClick={onClickApplicationSidebarExpand}
                isActive={isApplicationSidebarExpanded}
            />
            <ApplicationSidebarNavigation expanded={isApplicationSidebarExpanded}>
                {!!dmBaseUrl && (
                    <>
                        <TitleSubsection>{t("navigation.side.dmBrowser", "Browse in DataManager")}</TitleSubsection>
                        <Menu>
                            {dmModuleLinks ? (
                                dmModuleLinks.map((link) => (
                                    <MenuItem
                                        icon={link.icon}
                                        text={t("navigation.side.dm." + link.path, link.defaultLabel)}
                                        href={dmBaseUrl + "/" + link.path}
                                        key={link.path}
                                    />
                                ))
                            ) : (
                                <MenuItem text="DataManager" href={dmBaseUrl} />
                            )}
                        </Menu>
                        <Divider addSpacing="xlarge" />
                    </>
                )}
                <TitleSubsection>{t("navigation.side.diBrowse", "Create in DataIntegration")}</TitleSubsection>
                <Menu>
                    <MenuItem
                        icon="artefact-project"
                        text={t("navigation.side.di.projects", "Projects")}
                        onClick={() => handleNavigate("project")}
                    />
                    <MenuItem
                        icon="artefact-dataset"
                        text={t("navigation.side.di.datasets", "Datasets")}
                        onClick={() => handleNavigate("dataset")}
                    />
                </Menu>
            </ApplicationSidebarNavigation>

            <WorkspaceHeader id={APP_VIEWHEADER_ID} />

            <ApplicationToolbar>
                <ApplicationToolbarSection>
                    <CreateButton onClick={handleCreateDialog} />
                </ApplicationToolbarSection>
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
                                <MenuItem
                                    text={t("common.action.backOld", "Back to old workspace")}
                                    href={CONTEXT_PATH + "/workspace"}
                                    icon={"application-legacygui"}
                                />
                                <MenuItem
                                    text={t("common.action.activity", "Activity overview")}
                                    href={CONTEXT_PATH + "/workspace/allActivities"}
                                    icon={"application-activities"}
                                />
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
                                            triggerHotkeyHandler(hotKeys.quickSearch);
                                        }}
                                        icon={"operation-search"}
                                    />
                                )}
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
