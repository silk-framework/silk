import React, { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { commonOp, commonSel } from "@ducks/common";
import {
    ApplicationHeader,
    ApplicationSidebarToggler,
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
import { IProjectOrTask, ItemDeleteModal } from "../../shared/modals/ItemDeleteModal";
import { CONTEXT_PATH } from "../../../constants/path";

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

function HeaderComponent({ breadcrumbs, onClickApplicationSidebarExpand, isApplicationSidebarExpanded }: IProps) {
    const dispatch = useDispatch();
    const location = useLocation();

    const isAuth = useSelector(commonSel.isAuthSelector);

    const [deleteModalOpen, setDeleteModalOpen] = useState(false);
    const [selectedItem, setSelectedItem] = useState<IProjectOrTask | null>(null);

    const projectId = useSelector(commonSel.currentProjectIdSelector);
    const taskId = useSelector(commonSel.currentTaskIdSelector);
    const appliedFilters = useSelector(workspaceSel.appliedFiltersSelector);

    const startTitle = `Build — ${APPLICATION_SUITE_NAME}`;

    const [windowTitle, setWindowTitle] = useState<string>(startTitle);
    const [displayUserMenu, toggleUserMenuDispay] = useState<boolean>(false);

    const lastBreadcrumb = breadcrumbs[breadcrumbs.length - 1];

    useEffect(() => {
        getWindowTitle(projectId);
    }, [projectId, taskId, breadcrumbs]);

    const handleCreateDialog = () => {
        dispatch(commonOp.setSelectedArtefactDType(appliedFilters.itemType));
    };

    const onOpenDeleteModal = (item: IProjectOrTask) => {
        setSelectedItem(item);
        setDeleteModalOpen(true);
    };

    const onCloseDeleteModal = () => {
        setSelectedItem(null);
        setDeleteModalOpen(false);
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

    /*
        TODO: this is only a simple test to have a workaround for a while, we need
        to remove the check for iFrameDetection later again.
    */
    const iFrameDetection = window === window.parent;

    return !isAuth ? null : (
        <ApplicationHeader aria-label={APPLICATION_SUITE_NAME + ": " + APPLICATION_NAME}>
            {iFrameDetection && (
                <ApplicationSidebarToggler
                    aria-label="Open menu"
                    onClick={onClickApplicationSidebarExpand}
                    isActive={isApplicationSidebarExpanded}
                />
            )}
            {iFrameDetection && <ApplicationTitle prefix="eccenca">{APPLICATION_NAME}</ApplicationTitle>}
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
                        {(projectId || taskId) && (
                            <IconButton
                                name="item-remove"
                                text={taskId ? "Remove task" : "Remove project"}
                                disruptive
                                onClick={() =>
                                    onOpenDeleteModal({
                                        id: taskId ? taskId : projectId,
                                        projectId: taskId ? projectId : undefined,
                                    })
                                }
                            />
                        )}
                        <ContextMenu>
                            <MenuItem text={"This"} disabled />
                            <MenuItem text={"Is just a"} disabled />
                            <MenuItem text={"Dummy"} disabled />
                        </ContextMenu>
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
                                toggleUserMenuDispay(false);
                            }}
                        >
                            <Icon name="navigation-close" description="Close icon" large />
                        </ApplicationToolbarAction>
                        <ApplicationToolbarPanel aria-label="User menu" expanded={true}>
                            <Menu>
                                <MenuItem text={"Back to old workspace"} href={CONTEXT_PATH + "/workspace"} />
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
                            toggleUserMenuDispay(true);
                        }}
                    >
                        <Icon name="application-useraccount" description="User menu icon" large />
                    </ApplicationToolbarAction>
                )}
            </ApplicationToolbar>
            <CreateArtefactModal />
            {deleteModalOpen && selectedItem && (
                <ItemDeleteModal selectedItem={selectedItem} onClose={onCloseDeleteModal} />
            )}
        </ApplicationHeader>
    );
}

export const Header = withBreadcrumbLabels(HeaderComponent);
