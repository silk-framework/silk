import React, { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { useLocation } from "react-router";
import { useTranslation } from "react-i18next";
import { routerOp } from "@ducks/router";
import { IItemLink } from "@ducks/shared/typings";
import { commonOp, commonSel } from "@ducks/common";
import { requestItemLinks } from "@ducks/shared/requests";
import { IExportTypes } from "@ducks/common/typings";
import { downloadProject } from "../../../utils/downloadProject";
import { DATA_TYPES } from "../../../constants";
import { ItemDeleteModal } from "../modals/ItemDeleteModal";
import CloneModal from "../modals/CloneModal";
import { ActionsMenu, TActionsMenuItem, IActionsMenuProps } from "./ActionsMenu";
import CopyToModal from "../modals/CopyToModal/CopyToModal";
import ShowIdentifierModal from "../modals/ShowIdentifierModal";
import { SERVE_PATH } from "../../../constants/path";
import { absoluteProjectPath } from "../../../utils/routerUtils";

interface IProps {
    projectId: string;
    // If the task ID is set then this is a task else a project
    taskId?: string;
    itemType: string;
    updateActionsMenu: (actionMenu: JSX.Element) => any;
    // Called with true when the item links endpoint returns a 404
    notFoundCallback?: (boolean) => any;
}

export function ArtefactManagementOptions({
    projectId,
    taskId,
    itemType,
    updateActionsMenu,
    notFoundCallback = () => {},
}: IProps) {
    const dispatch = useDispatch();
    const location = useLocation<any>();
    const [t] = useTranslation();
    const [deleteModalOpen, setDeleteModalOpen] = useState(false);
    const [cloneModalOpen, setCloneModalOpen] = useState(false);
    const [copyToModalOpen, setCopyToModalOpen] = useState<boolean>(false);
    const [showIdentifierOpen, setShowIdentifierOpen] = useState<boolean>(false);
    const [itemLinks, setItemLinks] = useState<IItemLink[]>([]);
    const [menuItems, setMenuItems] = useState<IActionsMenuProps>({});
    const exportTypes = useSelector(commonSel.exportTypesSelector);

    const itemData = {
        id: taskId,
        projectId: projectId,
        type: itemType,
    };

    // Update item links for more menu
    useEffect(() => {
        if (projectId && taskId) {
            getItemLinks(taskId);
        } else {
            setItemLinks([]);
        }
    }, [projectId, taskId]);

    const getItemLinks = async (taskId: string) => {
        try {
            const { data } = await requestItemLinks(projectId, taskId);
            // remove current page link
            const result = data.filter((item) => item.path !== location.pathname);
            setItemLinks(result);
            notFoundCallback(false);
        } catch (e) {
            if (e?.httpStatus === 404) {
                notFoundCallback(true);
            }
        }
    };

    const handleDeleteConfirm = () => {
        toggleDeleteModal();
        let afterPage = "";
        if (taskId) {
            afterPage = absoluteProjectPath(projectId);
        } else {
            // Deleted project, workspace state may have changed
            dispatch(commonOp.fetchCommonSettingsAsync());
            afterPage = SERVE_PATH + "?itemType=project&page=1&limit=10";
        }
        dispatch(routerOp.goToPage(afterPage));
    };

    const handleCloneConfirmed = (id, detailsPage) => {
        toggleCloneModal();
        dispatch(routerOp.goToPage(detailsPage));
    };

    const handleProjectExport = (type: IExportTypes) => {
        downloadProject(itemData.projectId, type.id);
    };

    const handleCopyConfirmed = () => {
        toggleCopyToModal();
    };

    const toggleShowIdentifierModal = () => {
        setShowIdentifierOpen((show) => !show);
    };

    const toggleDeleteModal = () => {
        setDeleteModalOpen(!deleteModalOpen);
    };

    const toggleCloneModal = () => {
        setCloneModalOpen(!cloneModalOpen);
    };

    const toggleCopyToModal = () => {
        setCopyToModalOpen(!copyToModalOpen);
    };

    const getFullMenu = () => {
        const fullMenu: TActionsMenuItem[] = [
            {
                icon: "item-copy",
                text: t("common.action.copy", "Copy to"),
                actionHandler: toggleCopyToModal,
                "data-test-id": "header-copy-button",
            },
            {
                icon: "item-clone",
                text: t("common.action.clone", "Clone"),
                actionHandler: toggleCloneModal,
                "data-test-id": "header-clone-button",
            },
            {
                icon: "item-viewdetails",
                text: t("common.action.showIdentifier", "Show identifier"),
                actionHandler: toggleShowIdentifierModal,
                "data-test-id": "header-item-identifier-button",
            },
        ];

        if (itemType === DATA_TYPES.PROJECT) {
            const projectPath = `projects/${projectId}/activities?page=1&limit=25&sortBy=recentlyUpdated&sortOrder=ASC`;
            fullMenu.push({
                icon: "application-activities",
                text: t("widget.ActivityInfoWidget.title", "Activities"),
                "data-test-id": "header-item-activities-button",
                actionHandler: (e) => {
                    e.preventDefault();
                    e.stopPropagation();
                    dispatch(routerOp.goToPage(projectPath));
                },
            });
        }

        if (itemType === DATA_TYPES.PROJECT && !!exportTypes.length) {
            const subitems: { text: string; actionHandler: () => any }[] = [];
            exportTypes.forEach((type) => {
                subitems.push({
                    text: type.label,
                    actionHandler: () => handleProjectExport(type),
                });
            });

            fullMenu.push({
                text: t("common.action.exportTo", "Export to"),
                subitems: subitems,
            });
        }

        return fullMenu;
    };

    useEffect(() => {
        setMenuItems({
            actionsSecondary:
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
                    : [],
            actionsFullMenu: projectId || taskId ? getFullMenu() : [],
        });
    }, [projectId, taskId, itemType, exportTypes, itemLinks, t]);

    useEffect(() => {
        updateActionsMenu(<ActionsMenu {...menuItems} />);
    }, [menuItems]);

    return (
        <>
            {deleteModalOpen && (
                <ItemDeleteModal item={itemData} onClose={toggleDeleteModal} onConfirmed={handleDeleteConfirm} />
            )}
            {cloneModalOpen && (
                <CloneModal item={itemData} onDiscard={toggleCloneModal} onConfirmed={handleCloneConfirmed} />
            )}
            {copyToModalOpen && (
                <CopyToModal item={itemData} onDiscard={toggleCopyToModal} onConfirmed={handleCopyConfirmed} />
            )}
            {showIdentifierOpen && (
                <ShowIdentifierModal onDiscard={toggleShowIdentifierModal} taskId={taskId} projectId={projectId} />
            )}
            {/* {displayItemLink && (
                <ProjectTaskTabView
                    srcLinks={itemLinks}
                    startWithLink={displayItemLink}
                    startFullscreen={true}
                    handlerRemoveModal={() => toggleItemLink(null)}
                />
            )} */}
        </>
    );
}
