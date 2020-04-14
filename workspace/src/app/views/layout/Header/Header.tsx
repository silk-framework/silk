import React, { memo, useState } from 'react';
import { useDispatch, useSelector } from "react-redux";
import { globalOp, globalSel } from "@ducks/global";
import {
    ApplicationHeader,
    ApplicationSidebarToggler,
    ApplicationTitle,
    ApplicationToolbar,
    ApplicationToolbarSection,
    ApplicationToolbarAction,
    ApplicationToolbarPanel,
    WorkspaceHeader,
    OverviewItem,
    OverviewItemDepiction,
    OverviewItemDescription,
    OverviewItemLine,
    OverviewItemActions,
    TitlePage,
    Icon,
    IconButton,
    Button,
    ContextMenu,
    MenuItem,
    BreadcrumbList,
} from "@wrappers/index";
import HomeButton from "./HomeButton";
import CreateButton from "../../shared/buttons/CreateButton";
import { CreateArtefactModal } from "../../shared/modals/CreateArtefactModal/CreateArtefactModal";

interface IProps {
    externalRoutes: any;
    onClickApplicationSidebarExpand: any;
    isApplicationSidebarExpanded: any;
}

export const Header = ({onClickApplicationSidebarExpand, isApplicationSidebarExpanded}: IProps) => {
    const dispatch = useDispatch();
    const breadcrumbs = useSelector(globalSel.breadcrumbsSelector);

    const handleCreateDialog = () => {
        dispatch(globalOp.selectArtefact())
    };

    const isAuth = useSelector(globalSel.isAuthSelector);
    const lastBreadcrumb = breadcrumbs[breadcrumbs.length - 1];
    return (
        !isAuth ? null :
            <ApplicationHeader aria-label={"TODO: eccenca DI"}>
                <ApplicationSidebarToggler
                    aria-label="TODO: Open menu"
                    onClick={onClickApplicationSidebarExpand}
                    isActive={isApplicationSidebarExpanded}
                />
                <ApplicationTitle prefix="eccenca">DataIntegration</ApplicationTitle>
                <WorkspaceHeader>
                    <OverviewItem>
                        <OverviewItemDepiction>
                            <HomeButton/>
                        </OverviewItemDepiction>
                        <OverviewItemDescription>
                            <OverviewItemLine small>
                                <BreadcrumbList items={breadcrumbs} />
                            </OverviewItemLine>
                                {
                                    lastBreadcrumb &&
                                    <OverviewItemLine large>
                                        <TitlePage>{lastBreadcrumb.text}</TitlePage>
                                    </OverviewItemLine>
                                }
                        </OverviewItemDescription>
                        <OverviewItemActions>
                            <Button text="Dummy" outlined={'true'} elevated />
                            <IconButton name="item-remove" text="Remove" disruptive />
                            <ContextMenu>
                                <MenuItem text={'This'} disabled />
                                <MenuItem text={'Is just a'} disabled />
                                <MenuItem text={'Dummy'} disabled />
                            </ContextMenu>
                        </OverviewItemActions>
                    </OverviewItem>
                </WorkspaceHeader>
                <ApplicationToolbar>
                    <ApplicationToolbarSection>
                        <CreateButton onClick={handleCreateDialog}/>
                    </ApplicationToolbarSection>
                    <ApplicationToolbarAction
                        aria-label="TODO: User menu"
                        isActive={false}
                        onClick={() => {}}
                    >
                        <Icon
                            name="application-useraccount"
                            description="TODO: Open user menu"
                            large
                        />
                    </ApplicationToolbarAction>
                    <ApplicationToolbarPanel
                        aria-label="TODO: User panel"
                        expanded={false}
                    >
                        TODO
                    </ApplicationToolbarPanel>
                </ApplicationToolbar>
                <CreateArtefactModal />
            </ApplicationHeader>
    )
};
