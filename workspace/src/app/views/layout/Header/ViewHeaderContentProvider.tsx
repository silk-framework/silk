import React, { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { useLocation } from "react-router";
import { useTranslation } from "react-i18next";
import { Helmet } from "react-helmet";
import { IPageLabels } from "@ducks/router/operations";
import { IItemLink } from "@ducks/shared/typings";
import { IExportTypes } from "@ducks/common/typings";
import { routerOp } from "@ducks/router";
import { commonSel } from "@ducks/common";
import { requestItemLinks, requestTaskItemInfo } from "@ducks/shared/requests";
import { ItemDeleteModal } from "../../shared/modals/ItemDeleteModal";
import CloneModal from "../../shared/modals/CloneModal";
import { IframeWindow } from "../../shared/IframeWindow/IframeWindow";
import { downloadResource } from "../../../utils/downloadResource";
import { APPLICATION_CORPORATION_NAME, APPLICATION_SUITE_NAME } from "../../../constants/base";
import { DATA_TYPES } from "../../../constants";
import withBreadcrumbLabels from "./withBreadcrumbLabels";
import { ViewHeader } from "./ViewHeader";

export interface IBreadcrumb {
    href: string;
    text: string;
}

interface IViewHeaderContentProviderProps {
    breadcrumbs?: IBreadcrumb[];
}

function ViewHeaderContentProviderComponent({ breadcrumbs }: IViewHeaderContentProviderProps) {
    const dispatch = useDispatch();
    const location = useLocation<any>();
    const [t] = useTranslation();
    const exportTypes = useSelector(commonSel.exportTypesSelector);
    const taskId = useSelector(commonSel.currentTaskIdSelector);
    const projectId = useSelector(commonSel.currentProjectIdSelector);
    const lastBreadcrumb = breadcrumbs[breadcrumbs.length - 1];
    const startTitle = `${t("common.app.build")} — ${APPLICATION_CORPORATION_NAME} ${APPLICATION_SUITE_NAME}`;

    const [windowTitle, setWindowTitle] = useState<string>(startTitle);
    const [displayItemLink, setDisplayItemLink] = useState<IItemLink | null>(null);
    const [itemLinks, setItemLinks] = useState<IItemLink[]>([]);
    const [itemType, setItemType] = useState<string | null>(null);
    const [deleteModalOpen, setDeleteModalOpen] = useState(false);
    const [cloneModalOpen, setCloneModalOpen] = useState(false);

    const itemData = {
        id: taskId ? taskId : projectId,
        projectId: taskId ? projectId : undefined,
        type: itemType ? itemType : undefined,
    };

    // Set window title
    useEffect(() => {
        getWindowTitle(projectId);
    }, [projectId, taskId, lastBreadcrumb ? lastBreadcrumb.href : ""]);

    // Update item links for more menu
    useEffect(() => {
        if (projectId && taskId) {
            getItemLinks();
        } else {
            setItemLinks([]);
        }
    }, [projectId, taskId]);

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
            } (${datasetType}) at ${breadcrumbWithoutTitle} – ${APPLICATION_CORPORATION_NAME} ${APPLICATION_SUITE_NAME}`;
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

    const updateItemType = async (pageLabels: IPageLabels, locationPathName: string) => {
        if (projectId && taskId) {
            try {
                const response = await requestTaskItemInfo(projectId, taskId);
                const itemType = response.data.itemType.id;
                if (window.location.pathname === locationPathName) {
                    setItemType(itemType);
                }
            } catch (ex) {
                // Swallow exception, nothing we can do
            }
        }
    };

    const handleExport = (type: IExportTypes) => {
        downloadResource(itemData.id, type.id);
    };

    const handleDeleteConfirm = () => {
        toggleDeleteModal();
        let afterPage = "";
        if (taskId) {
            afterPage = `projects/${projectId}`;
        }
        dispatch(routerOp.goToPage(afterPage));
    };

    const handleCloneConfirmed = (id, detailsPage) => {
        toggleCloneModal();
        dispatch(routerOp.goToPage(detailsPage));
    };

    // handler for link change
    const toggleItemLink = (linkItem: IItemLink | null = null) => {
        setDisplayItemLink(linkItem);
    };

    const toggleDeleteModal = () => {
        setDeleteModalOpen(!deleteModalOpen);
    };

    const toggleCloneModal = () => {
        setCloneModalOpen(!cloneModalOpen);
    };

    const getFullMenu = () => {
        // TODO: typescript code check throws error when not each member has the same attributes, even if they are not necessary in every single item
        const fullMenu = [
            {
                text: t("common.action.clone", "Clone"),
                actionHandler: toggleCloneModal,
                // subitems: [],
                "data-test-id": "header-clone-button",
            },
        ];

        if (itemType === DATA_TYPES.PROJECT && !!exportTypes.length) {
            const subitems = [];
            exportTypes.forEach((type) => {
                subitems.push({
                    text: type.label,
                    actionHandler: () => handleExport(type),
                });
            });

            fullMenu.push({
                text: t("common.action.exportTo", "Export to"),
                // actionHandler: () => {},
                subitems: subitems,
                // "data-test-id": "",
            });
        }

        if (itemLinks && itemLinks.length > 0) {
            itemLinks.forEach((itemLink) => {
                fullMenu.push({
                    text: t("common.legacyGui." + itemLink.label, itemLink.label),
                    actionHandler: () => toggleItemLink(itemLink),
                    // subitems: [],
                    // "data-test-id": "",
                });
            });
        }

        return fullMenu;
    };

    return (
        <>
            <Helmet title={windowTitle} />
            <ViewHeader
                depiction={itemType ? "artefact-" + itemType : "application-homepage"}
                breadcrumbs={breadcrumbs}
                pagetitle={lastBreadcrumb ? lastBreadcrumb.text : ""}
                actionsSecondary={
                    projectId || taskId
                        ? [
                              {
                                  text: t("common.action.RemoveSmth", {
                                      smth: taskId
                                          ? t("common.dataTypes.task", "Task")
                                          : t("common.dataTypes.project", "Project"),
                                  }),
                                  icon: "item-remove",
                                  actionHandler: toggleDeleteModal,
                                  disruptive: true,
                                  "data-test-id": "header-remove-button",
                              },
                          ]
                        : []
                }
                actionsFullMenu={projectId || taskId ? getFullMenu() : []}
            />
            {deleteModalOpen && (
                <ItemDeleteModal item={itemData} onClose={toggleDeleteModal} onConfirmed={handleDeleteConfirm} />
            )}

            {cloneModalOpen && (
                <CloneModal item={itemData} onDiscard={toggleCloneModal} onConfirmed={handleCloneConfirmed} />
            )}
            {displayItemLink && (
                <IframeWindow
                    srcLinks={itemLinks}
                    startWithLink={displayItemLink}
                    startFullscreen={true}
                    handlerRemoveModal={() => toggleItemLink(null)}
                />
            )}
        </>
    );
}

export const ViewHeaderContentProvider = withBreadcrumbLabels(ViewHeaderContentProviderComponent);
