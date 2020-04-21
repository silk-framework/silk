import React, { memo, useEffect, useState } from 'react';
import { useDispatch, useSelector } from "react-redux";
import { commonOp, commonSel } from "@ducks/common";
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
import { useHistory, useLocation, useParams, matchPath } from "react-router";
import appRoutes from "../../../appRoutes";
import { getFullRoutePath } from "../../../utils/routerUtils";
import { SERVE_PATH } from "../../../constants";
import { useTranslation } from "react-i18next";

interface IProps {
    externalRoutes: any;
    onClickApplicationSidebarExpand: any;
    isApplicationSidebarExpanded: any;
}

export interface IBreadcrumb {
    href: string;
    text: string;
}

export const Header = ({onClickApplicationSidebarExpand, isApplicationSidebarExpanded}: IProps) => {
    const dispatch = useDispatch();
    const location = useLocation();
    const [t] = useTranslation();

    const [breadcrumbs, setBreadcrumbs] = useState<IBreadcrumb[]>([]);

    useEffect(() => {
        // @TODO: Add label values for breadcrumbs
        const match = appRoutes
            .map(route => matchPath(location.pathname, {
                path: getFullRoutePath(route.path),
                exact: route.exact,
            })).filter(Boolean);

        if (match) {
            const {params, url}: any = match[0];
            const updatedBread = [
                {href: SERVE_PATH, text: t("common.home")},
                {href: SERVE_PATH, text: t('Data Integration')},
            ];
            if (params.projectId) {
                updatedBread.push({
                    href: getFullRoutePath(`/projects/${params.projectId}`),
                    text: params.projectId
                })
            }
            if (params.taskId) {
                updatedBread.push({
                    href: url,
                    text: params.taskId
                })
            }
            setBreadcrumbs(updatedBread);
        }


    }, [location.pathname]);

    const handleCreateDialog = () => {
        dispatch(commonOp.selectArtefact())
    };

    const isAuth = useSelector(commonSel.isAuthSelector);
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
