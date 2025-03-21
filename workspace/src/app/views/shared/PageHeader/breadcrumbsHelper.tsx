import React, { useDebugValue, useEffect, useState } from "react";
import { matchPath, useLocation } from "react-router";
import { useTranslation } from "react-i18next";
import { BreadcrumbList, BreadcrumbItemProps } from "@eccenca/gui-elements";
import { requestProjectMetadata, requestTaskMetadata } from "@ducks/shared/requests";
import appRoutes from "../../../appRoutes";
import { getFullRoutePath } from "../../../utils/routerUtils";
import { SERVE_PATH } from "../../../constants/path";

export function fetchBreadcrumbs(WrappedComponent) {
    // Valid breadcrumb IDs
    const breadcrumbOrder = ["projectId", "taskId"];
    // Mappings from breadcrumb IDs to breadcrumb label properties
    const breadcrumbIdMap = { projectId: "projectLabel", taskId: "taskLabel" };

    return function (props) {
        const location = useLocation<any>();
        const [breadcrumbs, setBreadcrumbs] = useState<BreadcrumbItemProps[]>([]);
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
                // Remove breadcrumbs on navigation before reloading, else they will lag behind
                setBreadcrumbs([]);

                updateBreadCrumbs(match);
            }
        }, [location.pathname, location.state]);

        const updateBreadCrumbs = async (match) => {
            const { params = {}, url }: any = match[0];

            const labelFunction = labelForBreadCrumb(params);
            const updatedBread = [
                {
                    href: getFullRoutePath("?itemType=project&page=1&limit=10"),
                    text: t("common.app.build", "Workbench"),
                },
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
            try {
                switch (breadcrumbId) {
                    case "projectId":
                        return requestProjectMetadata(params.projectId).then((metadata) => metadata.data.label);
                    case "taskId":
                        return requestTaskMetadata(params.taskId, params.projectId).then(
                            (metadata) => metadata.data.label || metadata.data.id
                        );
                    default: {
                        return params[breadcrumbId];
                    }
                }
            } catch (ex) {
                return params[breadcrumbId];
            }
        };

        // Returns a function that returns the label for a specific breadcrumb ID
        const labelForBreadCrumb = (params: any): ((string) => Promise<string>) => {
            const actualBreadcrumbs = breadcrumbOrder.filter((breadcrumbId) => params[breadcrumbId]);
            const pageLabels = location.state?.pageLabels;
            const resultLabels = Object.create(null);
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

export const GeneratedBreadcrumbList = fetchBreadcrumbs(BreadcrumbList);

// Custom hook to get autogenerated BreadcrumbList and its data
export function useGeneratedBreadcrumbs() {
    const generatedBreadcrumbs = <GeneratedBreadcrumbList />;
    return {
        generatedBreadcrumbs,
    } as const;
}
