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
    ContextMenu,
    Icon,
    IconButton,
    Menu,
    MenuDivider,
    MenuItem,
    OverviewItem,
    OverviewItemActions,
    OverviewItemDepiction,
    OverviewItemDescription,
    OverviewItemLine,
    TitlePage,
    WorkspaceHeader,
} from "@wrappers/index";
import HomeButton from "./HomeButton";
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
import { requestItemLinks } from "@ducks/shared/requests";

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
    const location = useLocation();

    const isAuth = useSelector(commonSel.isAuthSelector);

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

    useEffect(() => {
        getWindowTitle(projectId);
        if (projectId && taskId) {
            getItemLinks();
        }
    }, [projectId, taskId, breadcrumbs]);

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

            let datasetType = "project";
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
                <Helmet>
                    <title>{windowTitle}</title>
                </Helmet>
                <OverviewItem>
                    <OverviewItemDepiction>
                        <HomeButton />
                    </OverviewItemDepiction>
                    <OverviewItemDescription>
                        <OverviewItemLine small>
                            <BreadcrumbList items={breadcrumbs} />
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
                                    text={taskId ? "Remove task" : "Remove project"}
                                    disruptive
                                    onClick={toggleDeleteModal}
                                />
                                <ContextMenu>
                                    <MenuItem key={"clone"} text={"Clone"} onClick={toggleCloneModal} />
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
                                <MenuItem text={"Back to old workspace"} href={CONTEXT_PATH + "/workspace"} />
                                <MenuItem text={"Activity overview"} href={CONTEXT_PATH + "/workspace/allActivities"} />
                                {iFrameDetection && (
                                    <>
                                        <MenuDivider />
                                        <MenuItem
                                            text="Logout"
                                            onClick={() => {
                                                dispatch(commonOp.logout());
                                            }}
                                        />
                                    </>
                                )}
                            </Menu>
                        </ApplicationToolbarPanel>
                    </>
                ) : (
                    <ApplicationToolbarAction
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
