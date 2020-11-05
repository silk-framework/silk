import React, { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import Store from "store";
import { commonOp, commonSel } from "@ducks/common";
import {
    ApplicationHeader,
    ApplicationSidebarNavigation,
    ApplicationSidebarToggler,
    ApplicationTitle,
    ApplicationToolbar,
    ApplicationToolbarAction,
    ApplicationToolbarPanel,
    ApplicationToolbarSection,
    BreadcrumbList,
    Button,
    ContextMenu,
    Divider,
    Icon,
    IconButton,
    Menu,
    MenuDivider,
    MenuItem,
    OverflowText,
    OverviewItem,
    OverviewItemActions,
    OverviewItemDepiction,
    OverviewItemDescription,
    OverviewItemLine,
    TitlePage,
    TitleSubsection,
    WorkspaceHeader,
} from "@gui-elements/index";
import CreateButton from "../../shared/buttons/CreateButton";
import { CreateArtefactModal } from "../../shared/modals/CreateArtefactModal/CreateArtefactModal";
import withBreadcrumbLabels from "./withBreadcrumbLabels";
import { Helmet } from "react-helmet";
import { useLocation } from "react-router";
import { APPLICATION_CORPORATION_NAME, APPLICATION_NAME, APPLICATION_SUITE_NAME } from "../../../constants/base";
import { workspaceOp, workspaceSel } from "@ducks/workspace";
import { ItemDeleteModal } from "../../shared/modals/ItemDeleteModal";
import { CONTEXT_PATH, SERVE_PATH } from "../../../constants/path";
import CloneModal from "../../shared/modals/CloneModal";
import { routerOp } from "@ducks/router";
import { IItemLink } from "@ducks/shared/typings";
import { requestItemLinks, requestTaskItemInfo } from "@ducks/shared/requests";
import { IPageLabels } from "@ducks/router/operations";
import { DATA_TYPES } from "../../../constants";
import { IExportTypes } from "@ducks/common/typings";
import { downloadResource } from "../../../utils/downloadResource";
import { useTranslation } from "react-i18next";
import { triggerHotkeyHandler } from "../../shared/HotKeyHandler/HotKeyHandler";

interface IProps {
    breadcrumbs?: IBreadcrumb[];
    onClickApplicationSidebarExpand: any;
    isApplicationSidebarExpanded: boolean;
}

export interface IBreadcrumb {
    href: string;
    text: string;
}

function HeaderComponent({ breadcrumbs, onClickApplicationSidebarExpand, isApplicationSidebarExpanded }: IProps) {
    const dispatch = useDispatch();
    const location = useLocation<any>();
    const [itemType, setItemType] = useState<string | null>(null);
    const [currentLanguage, setCurrentLanguage] = useState(Store.get("locale"));
    const { hotKeys } = useSelector(commonSel.initialSettingsSelector);

    const isAuth = useSelector(commonSel.isAuthSelector);
    const exportTypes = useSelector(commonSel.exportTypesSelector);

    const [deleteModalOpen, setDeleteModalOpen] = useState(false);
    const [cloneModalOpen, setCloneModalOpen] = useState(false);

    const projectId = useSelector(commonSel.currentProjectIdSelector);
    const taskId = useSelector(commonSel.currentTaskIdSelector);
    const { dmBaseUrl, dmModuleLinks } = useSelector(commonSel.initialSettingsSelector);
    const appliedFilters = useSelector(workspaceSel.appliedFiltersSelector);

    const lastBreadcrumb = breadcrumbs[breadcrumbs.length - 1];
    const [t] = useTranslation();

    const startTitle = `${t("common.app.build")} — ${APPLICATION_CORPORATION_NAME} ${APPLICATION_SUITE_NAME}`;

    const [windowTitle, setWindowTitle] = useState<string>(startTitle);
    const [displayUserMenu, toggleUserMenuDisplay] = useState<boolean>(false);
    const [itemLinks, setItemLinks] = useState<IItemLink[]>([]);

    const itemData = {
        id: taskId ? taskId : projectId,
        projectId: taskId ? projectId : undefined,
    };

    // Update task type
    useEffect(() => {
        if (location.state?.pageLabels?.itemType) {
            setItemType(location.state.pageLabels.itemType);
        } else {
            if (projectId && !taskId) {
                setItemType(DATA_TYPES.PROJECT);
            } else if (projectId && taskId) {
                if (!location.state?.pageLabels) {
                    location.state = { ...location.state };
                    location.state.pageLabels = {};
                }
                updateItemType(location.state.pageLabels, location.pathname);
            } else {
                setItemType(null);
            }
        }
    }, [projectId, taskId]);

    // Update item links for more menu
    useEffect(() => {
        getWindowTitle(projectId);
        if (projectId && taskId) {
            getItemLinks();
        } else {
            setItemLinks([]);
        }
    }, [projectId, taskId]);

    const updateItemType = async (pageLabels: IPageLabels, locationPathName: string) => {
        if (projectId && taskId) {
            try {
                const response = await requestTaskItemInfo(projectId, taskId);
                const itemType = response.data.itemType.id;
                pageLabels.itemType = itemType;
                if (window.location.pathname === locationPathName) {
                    setItemType(itemType);
                }
            } catch (ex) {
                // Swallow exception, nothing we can do
            }
        }
    };

    const handleCreateDialog = () => {
        dispatch(commonOp.setSelectedArtefactDType(appliedFilters.itemType));
    };

    const toggleDeleteModal = () => {
        setDeleteModalOpen(!deleteModalOpen);
    };

    const toggleCloneModal = () => {
        setCloneModalOpen(!cloneModalOpen);
    };

    const handleDeleteConfirm = () => {
        toggleDeleteModal();
        dispatch(routerOp.goToPage(""));
    };

    const handleCloneConfirmed = (id, detailsPage) => {
        toggleCloneModal();
        dispatch(routerOp.goToPage(detailsPage));
    };

    const handleBreadcrumbItemClick = (itemUrl, e) => {
        e.preventDefault();
        if (itemUrl) {
            dispatch(routerOp.goToPage(itemUrl, {}));
        }
    };

    const getWindowTitle = (projectId) => {
        // $title ($artefactLabel) at $breadcrumbsWithoutTitle — $companyName $applicationTitle
        let fullTitle = startTitle;

        if (lastBreadcrumb && projectId) {
            // when projectId is provided
            const breadcrumbWithoutTitle = breadcrumbs
                .slice(0, breadcrumbs.length - 1)
                .map((o) => o.text)
                .join(" / ");

            // select datatype from the url /projectId/type/taskId pattern
            const paths = location.pathname.split("/");
            const projectInd = paths.indexOf(projectId);

            let datasetType = DATA_TYPES.PROJECT;
            // for task type it next to project id
            if (paths[projectInd + 1]) {
                datasetType = paths[projectInd + 1];
            }
            fullTitle = `${
                lastBreadcrumb.text ? lastBreadcrumb.text : ""
            } (${datasetType}) at ${breadcrumbWithoutTitle} – ${APPLICATION_CORPORATION_NAME} ${APPLICATION_SUITE_NAME}`;
        }
        setWindowTitle(fullTitle);
    };

    const getItemLinks = async () => {
        try {
            const { data } = await requestItemLinks(projectId, taskId);
            // remove current page link
            const result = data.filter((item) => item.path !== location.pathname);
            setItemLinks(result);
        } catch (e) {}
    };

    const handleExport = (type: IExportTypes) => {
        downloadResource(itemData.id, type.id);
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

            <WorkspaceHeader>
                <Helmet title={windowTitle} />
                <OverviewItem>
                    <OverviewItemDepiction>
                        <Icon name={itemType ? "artefact-" + itemType : "application-homepage"} large />
                    </OverviewItemDepiction>
                    <OverviewItemDescription>
                        <OverviewItemLine small>
                            <BreadcrumbList items={breadcrumbs} onItemClick={handleBreadcrumbItemClick} />
                        </OverviewItemLine>
                        {lastBreadcrumb && (
                            <OverviewItemLine large>
                                <TitlePage>{lastBreadcrumb.text}</TitlePage>
                            </OverviewItemLine>
                        )}
                    </OverviewItemDescription>
                    <OverviewItemActions>
                        {projectId || taskId ? (
                            <>
                                <IconButton
                                    name="item-remove"
                                    text={t("common.action.RemoveSmth", {
                                        smth: taskId
                                            ? t("common.dataTypes.task", "Task")
                                            : t("common.dataTypes.project", "Project"),
                                    })}
                                    disruptive
                                    onClick={toggleDeleteModal}
                                    data-test-id={"header-remove-button"}
                                />
                                <ContextMenu>
                                    <MenuItem
                                        key={"clone"}
                                        text={t("common.action.clone", "Clone")}
                                        onClick={toggleCloneModal}
                                        data-test-id={"header-clone-button"}
                                    />
                                    {itemType === DATA_TYPES.PROJECT && !!exportTypes.length && (
                                        <MenuItem key="export" text={t("common.action.exportTo", "Export to")}>
                                            {exportTypes.map((type) => (
                                                <MenuItem
                                                    key={type.id}
                                                    onClick={() => handleExport(type)}
                                                    text={
                                                        <OverflowText inline>{type.label}</OverflowText>
                                                        /* TODO: change this OverflowText later to a multiline=false option on MenuItem, seenms to be a new one*/
                                                    }
                                                />
                                            ))}
                                        </MenuItem>
                                    )}
                                    {itemLinks.map((itemLink) => (
                                        <MenuItem
                                            key={itemLink.path}
                                            text={itemLink.label}
                                            href={itemLink.path}
                                            target={itemLink.path.startsWith(SERVE_PATH) ? undefined : "_blank"}
                                        />
                                    ))}
                                </ContextMenu>
                            </>
                        ) : null}
                    </OverviewItemActions>
                </OverviewItem>
            </WorkspaceHeader>
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
                                />
                                <MenuItem
                                    text={t("common.action.activity", "Activity overview")}
                                    href={CONTEXT_PATH + "/workspace/allActivities"}
                                />
                                {hotKeys.quickSearch && (
                                    <MenuItem
                                        text={t("RecentlyViewedModal.title") + ` ('${hotKeys.quickSearch}')`}
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
            {deleteModalOpen && (
                <ItemDeleteModal item={itemData} onClose={toggleDeleteModal} onConfirmed={handleDeleteConfirm} />
            )}

            {cloneModalOpen && (
                <CloneModal item={itemData} onDiscard={toggleCloneModal} onConfirmed={handleCloneConfirmed} />
            )}
        </ApplicationHeader>
    );
}

export const Header = withBreadcrumbLabels(HeaderComponent);
