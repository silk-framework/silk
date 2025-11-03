import React, { useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { workspaceOp, workspaceSel } from "@ducks/workspace";
import { Button, Icon, Spacing, Notification } from "@eccenca/gui-elements";
import Pagination from "../Pagination";
import DataList from "../Datalist";
import CloneModal from "../modals/CloneModal";
import AppliedFacets from "../../pages/Workspace/AppliedFacets";
import SearchItem from "./SearchItem";
import EmptyList from "./EmptyList";
import { commonOp, commonSel } from "@ducks/common";
import { ItemDeleteModal } from "../modals/ItemDeleteModal";
import { routerOp } from "@ducks/router";
import { ISearchResultsServer } from "@ducks/workspace/typings";
import { useTranslation } from "react-i18next";
import CopyToModal from "../modals/CopyToModal/CopyToModal";
import { IModalItem } from "@ducks/shared/typings";
import ShowIdentifierModal from "../modals/ShowIdentifierModal";
import { IArtefactModal } from "@ducks/common/typings";
import { GlobalTableContext } from "../../../GlobalContextsWrapper";

/** Search list for the workspace/project page search. */
export function SearchList() {
    const dispatch = useDispatch();

    const pageSizes = [10, 25, 50, 100];

    const data = useSelector(workspaceSel.resultsSelector);
    const pagination = useSelector(workspaceSel.paginationSelector);
    const { globalTableSettings } = React.useContext(GlobalTableContext);
    const workspaceTableSettings = globalTableSettings["workbench"];
    const adaptedPagination = {
        ...pagination,
    };
    if (workspaceTableSettings.pageSize) {
        adaptedPagination.limit = workspaceTableSettings.pageSize;
    }
    const appliedFilters = useSelector(workspaceSel.appliedFiltersSelector);
    const isLoading = useSelector(workspaceSel.isLoadingSelector);
    const appliedFacets = useSelector(workspaceSel.appliedFacetsSelector);
    const [deleteModalOpen, setDeleteModalOpen] = useState(false);
    const [selectedItem, setSelectedItem] = useState<IModalItem | null>(null);
    const [showCloneModal, setShowCloneModal] = useState(false);
    const [copyToModalOpen, setCopyToModalOpen] = useState<boolean>(false);
    const [showIdentifierOpen, setShowIdentifierOpen] = useState<boolean>(false);
    const projectId = useSelector(commonSel.currentProjectIdSelector);
    const [t] = useTranslation();

    const onDiscardModals = () => {
        setSelectedItem(null);
        setShowCloneModal(false);
        setDeleteModalOpen(false);
        setCopyToModalOpen(false);
        setShowIdentifierOpen(false);
    };

    const fixItemIdSettings = (item) => {
        // provide only projectId for projects
        if (typeof item.projectId === "undefined") {
            const correctedItem = { ...item, projectId: item.id };
            delete correctedItem.id;
            return correctedItem;
        }
        return item;
    };

    const onOpenDuplicateModal = (item) => {
        setShowCloneModal(true);
        setSelectedItem(fixItemIdSettings(item));
    };

    const onOpenDeleteModal = (item: ISearchResultsServer) => {
        setDeleteModalOpen(true);
        setSelectedItem(fixItemIdSettings(item));
    };

    const toggleShowIdentifierModal = (item) => {
        setShowIdentifierOpen((show) => !show);
        setSelectedItem(fixItemIdSettings(item));
    };

    const onOpenCopyToModal = (item: ISearchResultsServer) => {
        setCopyToModalOpen(true);
        setSelectedItem(fixItemIdSettings(item));
    };

    const handleCloneConfirmed = (newLabel, detailsPage) => {
        onDiscardModals();
        dispatch(routerOp.goToPage(detailsPage));
    };

    const handleDeleted = (deletedProject: boolean) => {
        if (deletedProject) {
            // Deleted project, workspace state may have changed
            dispatch(commonOp.fetchCommonSettingsAsync());
        }
        dispatch(workspaceOp.fetchListAsync());
        onDiscardModals();
    };

    const handlePaginationOnChange = (n: number, pageSize: number) => {
        dispatch(workspaceOp.changePageOp(n));
        dispatch(workspaceOp.changeLimitOp(pageSize));
    };

    const handleCreateArtefact = () => {
        const itemToCreate: Pick<IArtefactModal, "selectedDType" | "newTaskPreConfiguration"> = {
            selectedDType: appliedFilters.itemType ?? "all",
        };
        if (projectId && itemToCreate.selectedDType === "all") {
            itemToCreate.selectedDType = "workflow";
            itemToCreate.newTaskPreConfiguration = {
                taskPluginId: "workflow",
                metaData: {
                    label: t("pages.workspace.firstWorkflow"),
                },
            };
        }
        dispatch(commonOp.createNewTask(itemToCreate));
    };

    const isEmpty = !isLoading && !data.length;

    const itemTypeLabel = () => {
        if (appliedFilters.itemType) {
            return t("common.dataTypes." + appliedFilters.itemType);
        } else {
            return t("common.dataTypes.genericArtefactLabel");
        }
    };

    // Show the "create" action when no search query or facets applied
    const emptyListWithoutFilters: boolean = isEmpty && !appliedFilters.textQuery && !appliedFacets.length;
    const promptCreate = !!projectId || appliedFilters.itemType === "project" || !appliedFilters.itemType;

    const firstItemLabel =
        projectId && appliedFilters.itemType == null ? t("common.dataTypes.workflow") : itemTypeLabel();
    const EmptyContainer = emptyListWithoutFilters ? (
        <EmptyList
            depiction={<Icon name={["artefact-" + appliedFilters.itemType]} large />}
            textInfo={
                <p>
                    {t("common.messages.noItems", {
                        items: appliedFilters.itemType ? appliedFilters.itemType : "items",
                    })}
                </p>
            }
            textCallout={
                promptCreate && (
                    <strong>
                        {t("common.messages.createFirstItems", {
                            items: firstItemLabel,
                        })}
                    </strong>
                )
            }
            actionButtons={
                promptCreate
                    ? [
                          <Button
                              data-test-id={"create-first-item-btn"}
                              key={"create"}
                              onClick={handleCreateArtefact}
                              elevated
                          >
                              {t("common.action.CreateSmth", { smth: firstItemLabel })}
                          </Button>,
                      ]
                    : []
            }
        />
    ) : (
        <Notification>{t("common.messages.noItems", { items: "items" })}</Notification>
    );
    return (
        <>
            <AppliedFacets />
            <DataList
                // FIXME: Just a workaround for stale item bug that happens when switching between activities and search view
                key={`search-list-${data[0]?.id}-${data.length}`}
                data-test-id="search-result-list"
                isEmpty={isEmpty}
                isLoading={isLoading}
                hasSpacing
                emptyContainer={EmptyContainer}
            >
                {data.map((item) => (
                    <SearchItem
                        key={`${item.id}_${item.projectId}`}
                        item={item}
                        onOpenDeleteModal={onOpenDeleteModal}
                        onOpenDuplicateModal={onOpenDuplicateModal}
                        onOpenCopyToModal={onOpenCopyToModal}
                        toggleShowIdentifierModal={toggleShowIdentifierModal}
                        searchValue={appliedFilters.textQuery}
                        parentProjectId={projectId}
                    />
                ))}
            </DataList>
            <Spacing size="small" />
            <Pagination
                pagination={adaptedPagination}
                pageSizes={pageSizes}
                onChangeSelect={handlePaginationOnChange}
            />
            {!isEmpty ? (
                <>
                    {deleteModalOpen && selectedItem && (
                        <ItemDeleteModal
                            item={selectedItem}
                            onClose={onDiscardModals}
                            onConfirmed={() => handleDeleted(!selectedItem.projectId)}
                        />
                    )}
                    {copyToModalOpen && selectedItem && (
                        <CopyToModal
                            item={selectedItem}
                            onDiscard={onDiscardModals}
                            onConfirmed={() => setCopyToModalOpen(false)}
                        />
                    )}
                    {showCloneModal && selectedItem && (
                        <CloneModal
                            item={selectedItem}
                            onDiscard={onDiscardModals}
                            onConfirmed={handleCloneConfirmed}
                        />
                    )}
                    {selectedItem && showIdentifierOpen && (
                        <ShowIdentifierModal
                            projectId={selectedItem.projectId}
                            taskId={selectedItem.type !== "project" ? selectedItem.id : undefined}
                            onDiscard={onDiscardModals}
                        />
                    )}
                </>
            ) : null}
        </>
    );
}
