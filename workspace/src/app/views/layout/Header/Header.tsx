import React, { memo, useState } from 'react';

import { globalOp, globalSel } from "@ducks/global";
import { useDispatch, useSelector } from "react-redux";
import Breadcrumbs from "@wrappers/blueprint/breadcrumbs";
import NavbarHeading from "@wrappers/blueprint/navbar-heading";
import HomeButton from "./HomeButton";
import {
    ApplicationHeader,
    ApplicationSidebarToggler,
    ApplicationTitle,
    ApplicationToolbar,
    ApplicationToolbarAction,
    ApplicationToolbarPanel,
    WorkspaceHeader,
    Icon,
} from "@wrappers/index";
import CreateButton from "./CreateButton";
import { CreateArtefactModal } from "../../components/modals/CreateArtefactModal/CreateArtefactModal";

interface IProps {
    externalRoutes: any;
    onClickApplicationSidebarExpand: any;
    isApplicationSidebarExpanded: any;
}

const Header = ({onClickApplicationSidebarExpand, isApplicationSidebarExpanded}: IProps) => {
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
                    <HomeButton/>
                    <div>
                        <Breadcrumbs paths={breadcrumbs}/>
                        {
                            lastBreadcrumb && <NavbarHeading style={{fontWeight: 'bold'}}>{lastBreadcrumb.text}</NavbarHeading>
                        }
                    </div>
                </WorkspaceHeader>
                <ApplicationToolbar>
                    <CreateButton onClick={handleCreateDialog}/>
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

export default Header;
