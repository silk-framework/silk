import React from "react";
import { matchPath, useLocation } from "react-router";
import { sharedOp } from "@ducks/shared";
import { useDebugValue, useEffect, useState } from "react";
import appRoutes from "../../../appRoutes";
import { getFullRoutePath } from "../../../utils/routerUtils";
import { SERVE_PATH } from "../../../constants";
import { IBreadcrumb } from "./Header";
import { useTranslation } from "react-i18next";

export default function withBreadcrumbLabels(WrappedComponent) {
    // Valid breadcrumb IDs
    const breadcrumbOrder = ["projectId", "taskId"];
    // Mappings from breadcrumb IDs to breadcrumb label properties
    const breadcrumbIdMap = { projectId: "projectLabel", taskId: "taskLabel" };

    return function (props) {
        const location = useLocation<any>();
        const [breadcrumbs, setBreadcrumbs] = useState<IBreadcrumb[]>([]);
        const [t] = useTranslation();

        useEffect(() => {
            const match = appRoutes
                .map((route) =>
                    matchPath(location.pathname, {
                        path: getFullRoutePath(route.path),
                        exact: route.exact,
                    })
                )
                .filter(Boolean);

            if (match) {
                updateBreadCrumbs(match);
            }
        }, [location.pathname, location.state]);

        const updateBreadCrumbs = async (match) => {
            const { params = {}, url }: any = match[0];

            const labelFunction = labelForBreadCrumb(params);
            const updatedBread = [
                { href: SERVE_PATH, text: t("common.home") },
                { href: SERVE_PATH, text: t("Data Integration") },
            ];

            if (params.projectId) {
                updatedBread.push({
                    href: getFullRoutePath(`/projects/${params.projectId}`),
                    // text: params.projectId
                    text: await labelFunction("projectId"),
                });
            }
            if (params.taskId) {
                updatedBread.push({
                    href: url,
                    // text: params.taskId,
                    text: await labelFunction("taskId"),
                });
            }

            setBreadcrumbs(updatedBread);
        };

        // Functions to fetch the label for a specific breadcrumb item
        const fetchLabel = async (breadcrumbId: string, params: any): Promise<string> => {
            switch (breadcrumbId) {
                case "projectId": {
                    return sharedOp.getTaskMetadataAsync(params.projectId).then((metadata) => metadata.label);
                }
                case "taskId": {
                    return sharedOp
                        .getTaskMetadataAsync(params.taskId, params.projectId)
                        .then((metadata) => metadata.label);
                }
                default: {
                    return params[breadcrumbId];
                }
            }
        };

        // Returns a function that returns the label for a specific breadcrumb ID
        const labelForBreadCrumb = (params: any): ((string) => Promise<string>) => {
            const actualBreadcrumbs = breadcrumbOrder.filter((breadcrumbId) => params[breadcrumbId]);
            const pageLabels = location.state?.pageLabels;
            const resultLabels = {};
            // Extract labels from location state if existent
            actualBreadcrumbs.forEach((breadcrumbId, idx) => {
                if (idx + 1 === actualBreadcrumbs.length && pageLabels?.pageTitle) {
                    resultLabels[breadcrumbId] = pageLabels.pageTitle;
                } else if (pageLabels && pageLabels[breadcrumbIdMap[breadcrumbId]]) {
                    resultLabels[breadcrumbId] = pageLabels[breadcrumbIdMap[breadcrumbId]];
                }
            });

            return async (breadcrumbId: string) => {
                if (resultLabels[breadcrumbId]) {
                    // Label exists in location state, use it.
                    return resultLabels[breadcrumbId];
                } else if (breadcrumbIdMap[breadcrumbId]) {
                    // Label does not exists, but it is a valid breadcrumb ID, fetch label from backend.
                    return await fetchLabel(breadcrumbId, params);
                } else {
                    // return the value for breadcrumb ID specified in params. We are not able to get a label for is yet.
                    useDebugValue(`Invalid breadcrumb ID for label substitution: '${breadcrumbId}'.`);
                    return params[breadcrumbId];
                }
            };
        };

        return <WrappedComponent breadcrumbs={breadcrumbs} {...props} />;
    };
}
