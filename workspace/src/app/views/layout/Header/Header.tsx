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
    Button,
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
import { matchPath, useLocation } from "react-router";
import appRoutes from "../../../appRoutes";
import { getFullRoutePath } from "../../../utils/routerUtils";
import { SERVE_PATH } from "../../../constants";
import { useTranslation } from "react-i18next";
import { sharedOp } from "@ducks/shared";

interface IProps {
    externalRoutes: any;
    onClickApplicationSidebarExpand: any;
    isApplicationSidebarExpanded: any;
}

export interface IBreadcrumb {
    href: string;
    text: string;
}

/** Utility methods for breadcrumbs in the application header. */
class HeaderBreadcrumb {
    // Valid breadcrumb IDs
    static breadcrumbOrder = ["projectId", "taskId"];
    // Mappings from breadcrumb IDs to breadcrumb label properties
    static breadcrumbIdMap = { projectId: "projectLabel", taskId: "taskLabel" };
    // Functions to fetch the label for a specific breadcrumb item
    static fetchLabel = async (breadcrumbId: string, params: any): Promise<string> => {
        switch (breadcrumbId) {
            case "projectId": {
                return sharedOp.getTaskMetadataAsync(params.projectId).then((metaData) => metaData.label);
            }
            case "taskId": {
                return sharedOp
                    .getTaskMetadataAsync(params.taskId, params.projectId)
                    .then((metaData) => metaData.label);
            }
            default: {
                return Promise.resolve(params[breadcrumbId]);
            }
        }
    };

    // Returns a function that returns the label for a specific breadcrumb ID
    static labelForBreadCrumb = (location, params: any): ((string) => Promise<string>) => {
        const actualBreadcrumbs = HeaderBreadcrumb.breadcrumbOrder.filter((breadcrumbId) => params[breadcrumbId]);
        const pageLabels = location.state?.pageLabels;
        const resultLabels = {};
        actualBreadcrumbs.forEach((breadcrumbId, idx) => {
            if (idx + 1 === actualBreadcrumbs.length && pageLabels?.pageTitle) {
                resultLabels[breadcrumbId] = pageLabels.pageTitle;
            } else if (pageLabels && pageLabels[HeaderBreadcrumb.breadcrumbIdMap[breadcrumbId]]) {
                resultLabels[breadcrumbId] = pageLabels[HeaderBreadcrumb.breadcrumbIdMap[breadcrumbId]];
            }
        });
        return async (breadcrumbId: string) => {
            if (resultLabels[breadcrumbId]) {
                // Label exists
                return resultLabels[breadcrumbId];
            } else if (HeaderBreadcrumb.breadcrumbIdMap[breadcrumbId]) {
                // Label does not exists, but it is a valid breadcrumb ID
                return HeaderBreadcrumb.fetchLabel(breadcrumbId, params);
            } else {
                // return the value for breadcrumb ID specified in params. We are not able to get a label for is yet.
                console.warn(`Invalid breadcrumb ID for label substitution: '${breadcrumbId}'.`);
                return params[breadcrumbId];
            }
        };
    };
}

export function Header({ onClickApplicationSidebarExpand, isApplicationSidebarExpanded }: IProps) {
    const dispatch = useDispatch();
    const location = useLocation();
    const [t] = useTranslation();

    const [breadcrumbs, setBreadcrumbs] = useState<IBreadcrumb[]>([]);

    useEffect(() => {
        // @TODO: Add label values for breadcrumbs
        const match = appRoutes
            .map((route) =>
                matchPath(location.pathname, {
                    path: getFullRoutePath(route.path),
                    exact: route.exact,
                })
            )
            .filter(Boolean);

        if (match) {
            const { params, url }: any = match[0];
            updateBreadCrumbs(location, params, url);
        }
    }, [location.pathname, t]);

    const updateBreadCrumbs = async (location, params: any, url: string) => {
        const labelFunction = HeaderBreadcrumb.labelForBreadCrumb(location, params);
        const updatedBread = [
            { href: SERVE_PATH, text: t("common.home") },
            { href: SERVE_PATH, text: t("Data Integration") },
        ];
        if (params.projectId) {
            updatedBread.push({
                href: getFullRoutePath(`/projects/${params.projectId}`),
                text: await labelFunction("projectId"),
            });
        }
        if (params.taskId) {
            updatedBread.push({
                href: url,
                text: await labelFunction("taskId"),
            });
        }
        setBreadcrumbs(updatedBread);
    };

    const handleCreateDialog = () => {
        dispatch(commonOp.selectArtefact({}));
    };

    const isAuth = useSelector(commonSel.isAuthSelector);
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
