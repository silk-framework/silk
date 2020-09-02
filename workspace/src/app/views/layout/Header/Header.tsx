import React, { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { commonOp, commonSel } from "@ducks/common";
import {
    ApplicationHeader,
    ApplicationTitle,
    ApplicationToolbar,
    ApplicationToolbarAction,
    ApplicationToolbarPanel,
    ApplicationToolbarSection,
    BreadcrumbList,
    Button,
    ContextMenu,
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
    WorkspaceHeader,
} from "@gui-elements/index";
import ItemDepiction from "./ItemDepiction";
import CreateButton from "../../shared/buttons/CreateButton";
import { CreateArtefactModal } from "../../shared/modals/CreateArtefactModal/CreateArtefactModal";
import withBreadcrumbLabels from "./withBreadcrumbLabels";
import { Helmet } from "react-helmet";
import { useLocation } from "react-router";
import { APPLICATION_NAME, APPLICATION_SUITE_NAME } from "../../../constants/base";
import { workspaceSel } from "@ducks/workspace";
import { ItemDeleteModal } from "../../shared/modals/ItemDeleteModal";
import { CONTEXT_PATH } from "../../../constants/path";
import CloneModal from "../../shared/modals/CloneModal";
import { routerOp } from "@ducks/router";
import { IItemLink } from "@ducks/shared/typings";
import { requestItemLinks, requestTaskItemInfo } from "@ducks/shared/requests";
import { IPageLabels } from "@ducks/router/operations";
import { DATA_TYPES } from "../../../constants";
import { IExportTypes } from "@ducks/common/typings";
import { downloadResource } from "../../../utils/downloadResource";
import { useTranslation } from "react-i18next";

interface IProps {
    breadcrumbs?: IBreadcrumb[];
    externalRoutes: any;
    onClickApplicationSidebarExpand: any;
    isApplicationSidebarExpanded: any;
}

export interface IBreadcrumb {
    href: string;
    text: string;
}

function HeaderComponent({ breadcrumbs }: IProps) {
    const dispatch = useDispatch();
    const location = useLocation<any>();
    const [itemType, setItemType] = useState<string | null>(null);

    const isAuth = useSelector(commonSel.isAuthSelector);
    const exportTypes = useSelector(commonSel.exportTypesSelector);

    const [deleteModalOpen, setDeleteModalOpen] = useState(false);
    const [cloneModalOpen, setCloneModalOpen] = useState(false);

    const projectId = useSelector(commonSel.currentProjectIdSelector);
    const taskId = useSelector(commonSel.currentTaskIdSelector);
    const appliedFilters = useSelector(workspaceSel.appliedFiltersSelector);

    const startTitle = `Build — ${APPLICATION_SUITE_NAME}`;

    const [windowTitle, setWindowTitle] = useState<string>(startTitle);
    const [displayUserMenu, toggleUserMenuDisplay] = useState<boolean>(false);
    const [itemLinks, setItemLinks] = useState<IItemLink[]>([]);

    const lastBreadcrumb = breadcrumbs[breadcrumbs.length - 1];
    const [t] = useTranslation();

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
            } (${datasetType}) at ${breadcrumbWithoutTitle} – ${APPLICATION_SUITE_NAME}`;
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

    /*
        TODO: this is only a simple test to have a workaround for a while, we need
        to remove the check for iFrameDetection later again.
    */
    const iFrameDetection = window === window.parent;

    const itemData = {
        id: taskId ? taskId : projectId,
        projectId: taskId ? projectId : undefined,
    };

    const handleExport = (type: IExportTypes) => {
        downloadResource(itemData.id, type.id);
    };

    const handleLanguageChange = (locale: string) => {
        dispatch(commonOp.changeLocale(locale));
    };

    return !isAuth ? null : (
        <ApplicationHeader aria-label={APPLICATION_SUITE_NAME + ": " + APPLICATION_NAME}>
            {/*
            // currently not needed because we currently don't have a menu
            {iFrameDetection && (
                <ApplicationSidebarToggler
                    aria-label="Open menu"
                    onClick={onClickApplicationSidebarExpand}
                    isActive={isApplicationSidebarExpanded}
                />
            )
            */}
            {
                /* TODO: only show when application menu is opened */
                iFrameDetection && (
                    <ApplicationTitle prefix="eccenca" className="bx--visually-hidden">
                        {APPLICATION_NAME}
                    </ApplicationTitle>
                )
            }
            <WorkspaceHeader>
                <Helmet title={windowTitle} />
                <OverviewItem>
                    <OverviewItemDepiction>
                        <ItemDepiction itemType={itemType} />
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
                                        <MenuItem key={itemLink.path} text={itemLink.label} href={itemLink.path} />
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
                                    <Button onClick={() => handleLanguageChange("en")}>En</Button>
                                    <Button onClick={() => handleLanguageChange("de")}>De</Button>
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
                                {iFrameDetection && (
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
