import React, { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { useHistory, useLocation } from "react-router";
import { useTranslation } from "react-i18next";
import { routerOp } from "@ducks/router";
import { IItemLink } from "@ducks/shared/typings";
import { commonOp, commonSel } from "@ducks/common";
import { requestItemLinks, requestTaskData } from "@ducks/shared/requests";
import { IExportTypes } from "@ducks/common/typings";
import { downloadProject } from "../../../utils/downloadProject";
import { DATA_TYPES } from "../../../constants";
import { ItemDeleteModal } from "../modals/ItemDeleteModal";
import CloneModal from "../modals/CloneModal";
import { ActionsMenu, IActionsMenuProps, TActionsMenuItem } from "./ActionsMenu";
import CopyToModal from "../modals/CopyToModal/CopyToModal";
import ShowIdentifierModal from "../modals/ShowIdentifierModal";
import { SERVE_PATH } from "../../../constants/path";
import { absoluteProjectPath } from "../../../utils/routerUtils";
import { AlertDialog, Button, HtmlContentBlock, Notification } from "@eccenca/gui-elements";
import { FetchError } from "../../../services/fetch/responseInterceptor";
import { clearDataset } from "@ducks/workspace/requests";

interface IProps {
    projectId: string;
    // If the task ID is set then this is a task else a project
    taskId?: string;
    itemType: string;
    updateActionsMenu: (actionMenu: JSX.Element) => any;
    // Called with true when the item links endpoint returns a 404
    notFoundCallback?: (boolean) => any;
    // Called with true when the item links endpoint returns a 403
    forbiddenCallback?: (boolean) => any;
}

export function ArtefactManagementOptions({
    projectId,
    taskId,
    itemType,
    updateActionsMenu,
    notFoundCallback = () => {},
    forbiddenCallback = () => {},
}: IProps) {
    const dispatch = useDispatch();
    const location = useLocation<any>();
    const history = useHistory();
    const [t] = useTranslation();
    const [deleteModalOpen, setDeleteModalOpen] = useState(false);
    const [cloneModalOpen, setCloneModalOpen] = useState(false);
    const [copyToModalOpen, setCopyToModalOpen] = useState<boolean>(false);
    const [showIdentifierOpen, setShowIdentifierOpen] = useState<boolean>(false);
    const [itemLinks, setItemLinks] = useState<IItemLink[]>([]);
    const [menuItems, setMenuItems] = useState<IActionsMenuProps>({});
    const [showClearDatasetPrompt, setShowClearDatasetPrompt] = React.useState<boolean>(false);
    const notifications = React.useRef<React.JSX.Element[]>([]);
    const [erasingDataset, setErasingDataset] = useState<boolean>(false);
    const [isReadOnlyDataset, setIsReadOnlyDataset] = useState<boolean | undefined>(undefined);

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
            if (itemType === DATA_TYPES.DATASET) {
                const checkReadOnly = async () => {
                    const response = await requestTaskData(projectId, taskId);
                    setIsReadOnlyDataset(response.data.data.readOnly);
                };
                checkReadOnly();
            }
        } else {
            setItemLinks([]);
        }
    }, [projectId, taskId, itemType]);

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
            } else if (e?.httpStatus === 403) {
                forbiddenCallback(true);
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

    const handleClearDataset = async () => {
        if (!projectId && !taskId) return;
        setErasingDataset(true);
        try {
            await clearDataset(projectId, taskId as string);
            history.go(0);
            setShowClearDatasetPrompt(false);
        } catch (err) {
            notifications.current.push(
                <Notification
                    message={
                        (err as FetchError)?.errorResponse?.detail ??
                        t("DataPreview.clearDatasetModal.error", "Error while clearing dataset")
                    }
                    intent="danger"
                />,
            );
        } finally {
            setErasingDataset(false);
            notifications.current = [];
        }
    };

    const ConfirmClearDatasetPrompt = () => {
        if (itemType !== DATA_TYPES.DATASET) {
            return null;
        }
        return (
            <AlertDialog
                isOpen={showClearDatasetPrompt}
                size="tiny"
                warning
                title={`${t("DataPreview.clearDatasetModal.title", "Clear dataset")}?`}
                actions={[
                    <Button key="1" affirmative onClick={handleClearDataset} loading={erasingDataset}>
                        {t("DataPreview.clearDatasetModal.actionBtn")}
                    </Button>,
                    <Button key="2" onClick={() => setShowClearDatasetPrompt(false)}>
                        {t("common.action.cancel")}
                    </Button>,
                ]}
                notifications={notifications.current}
            >
                <HtmlContentBlock>
                    <p>{t("DataPreview.clearDatasetModal.content")}</p>
                </HtmlContentBlock>
            </AlertDialog>
        );
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
            disruptiveActions:
                projectId && taskId && itemType === DATA_TYPES.DATASET
                    ? [
                          {
                              icon: "operation-erase",
                              text: t("DataPreview.clearDatasetModal.title", "Clear dataset"),
                              disabled: isReadOnlyDataset,
                              disruptive: true,
                              actionHandler: () => setShowClearDatasetPrompt(true),
                              "data-test-id": "header-item-erase-dataset-button",
                              tooltipText: isReadOnlyDataset ? t("DataPreview.clearDatasetModal.readOnly") : undefined,
                          },
                      ]
                    : [],
        });
    }, [projectId, taskId, itemType, exportTypes, itemLinks, t, isReadOnlyDataset]);

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
            <ConfirmClearDatasetPrompt />
        </>
    );
}
