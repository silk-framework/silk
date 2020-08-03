import React, { useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { workspaceOp, workspaceSel } from "@ducks/workspace";
import { Button, Icon, Spacing } from "@wrappers/index";
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

export function SearchList() {
    const dispatch = useDispatch();

    const pageSizes = [10, 25, 50, 100];

    const data = useSelector(workspaceSel.resultsSelector);
    const pagination = useSelector(workspaceSel.paginationSelector);
    const appliedFilters = useSelector(workspaceSel.appliedFiltersSelector);
    const isLoading = useSelector(workspaceSel.isLoadingSelector);
    const appliedFacets = useSelector(workspaceSel.appliedFacetsSelector);
    const [deleteModalOpen, setDeleteModalOpen] = useState(false);
    const [selectedItem, setSelectedItem] = useState<ISearchResultsServer | null>(null);
    const [showCloneModal, setShowCloneModal] = useState(false);
    const projectId = useSelector(commonSel.currentProjectIdSelector);
    const [t] = useTranslation();

    const onDiscardModals = () => {
        setShowCloneModal(false);
        setDeleteModalOpen(false);
        setSelectedItem(null);
    };

    const onOpenDuplicateModal = (item) => {
        setShowCloneModal(true);
        setSelectedItem(item);
    };

    const onOpenDeleteModal = (item: ISearchResultsServer) => {
        setDeleteModalOpen(true);
        setSelectedItem(item);
    };

    const handleCloneConfirmed = (newLabel, detailsPage) => {
        onDiscardModals();
        dispatch(routerOp.goToPage(detailsPage));
    };

    const handleDeleted = () => {
        dispatch(workspaceOp.fetchListAsync());
        onDiscardModals();
    };

    const handlePaginationOnChange = (n: number, pageSize: number) => {
        dispatch(workspaceOp.changePageOp(n));
        dispatch(workspaceOp.changeLimitOp(pageSize));
    };

    const handleCreateArtefact = () => {
        dispatch(commonOp.setSelectedArtefactDType(appliedFilters.itemType));
    };

    const isEmpty = !isLoading && !data.length;

    const itemTypeLabel = () => {
        if (appliedFilters.itemType) {
            return appliedFilters.itemType;
        } else {
            return "item";
        }
    };

    // Show the "create" action when no search query or facets applied
    const EmptyContainer =
        isEmpty && !appliedFilters.textQuery && !appliedFacets.length ? (
            <EmptyList
                depiction={<Icon name={"artefact-" + appliedFilters.itemType} large />}
                textInfo={
                    <p>
                        {t("common.messages.noItems", {
                            items: appliedFilters.itemType ? appliedFilters.itemType : "items",
                        })}
                    </p>
                }
                textCallout={<strong>{t("common.messages.createFirstItems", { items: itemTypeLabel() })}</strong>}
                actionButtons={[
                    <Button key={"create"} onClick={handleCreateArtefact} elevated>
                        {t("common.action.CreateSmth", { smth: appliedFilters.itemType || "" })}
                    </Button>,
                ]}
            />
        ) : (
            <p>{t("common.messages.noItems", { items: "items" })}</p>
        );

    return (
        <>
            <AppliedFacets />
            <DataList isEmpty={isEmpty} isLoading={isLoading} hasSpacing emptyContainer={EmptyContainer}>
                {data.map((item) => (
                    <SearchItem
                        key={`${item.id}_${item.projectId}`}
                        item={item}
                        onOpenDeleteModal={() => onOpenDeleteModal(item)}
                        onOpenDuplicateModal={() => onOpenDuplicateModal(item)}
                        searchValue={appliedFilters.textQuery}
                        parentProjectId={projectId}
                    />
                ))}
            </DataList>
            {!isEmpty ? (
                <>
                    <Spacing size="small" />

                    <Pagination
                        pagination={pagination}
                        pageSizes={pageSizes}
                        onChangeSelect={handlePaginationOnChange}
                    />

                    {deleteModalOpen && selectedItem && (
                        <ItemDeleteModal item={selectedItem} onClose={onDiscardModals} onConfirmed={handleDeleted} />
                    )}
                    {showCloneModal && selectedItem && (
                        <CloneModal
                            item={selectedItem}
                            onDiscard={onDiscardModals}
                            onConfirmed={handleCloneConfirmed}
                        />
                    )}
                </>
            ) : null}
        </>
    );
}
