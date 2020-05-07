import React from "react";
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

    const isAuth = useSelector(commonSel.isAuthSelector);

    const handleCreateDialog = () => {
        dispatch(commonOp.selectArtefact({}));
    };

    const lastBreadcrumb = breadcrumbs[breadcrumbs.length - 1];

    /*
        TODO: this is only a simple test to have a workaround for a while, we need
        to remove the check for iFrameDetection later again.
    */
    const iFrameDetection = window === window.parent ? true : false;

    return !isAuth ? null : (
        <ApplicationHeader aria-label={"TODO: eccenca DI"}>
            {iFrameDetection && (
                <ApplicationSidebarToggler
                    aria-label="TODO: Open menu"
                    onClick={onClickApplicationSidebarExpand}
                    isActive={isApplicationSidebarExpanded}
                />
            )}
            {iFrameDetection && <ApplicationTitle prefix="eccenca">DataIntegration</ApplicationTitle>}
            <WorkspaceHeader>
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
                        <IconButton name="item-remove" text="Remove" disruptive />
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
                {iFrameDetection && (
                    <ApplicationToolbarAction aria-label="TODO: User menu" isActive={false} onClick={() => {}}>
                        <Icon name="application-useraccount" description="TODO: Open user menu" large />
                    </ApplicationToolbarAction>
                )}
                <ApplicationToolbarPanel aria-label="TODO: User panel" expanded={false}>
                    TODO
                </ApplicationToolbarPanel>
            </ApplicationToolbar>
            <CreateArtefactModal />
        </ApplicationHeader>
    );
}

export const Header = withBreadcrumbLabels(HeaderComponent);
