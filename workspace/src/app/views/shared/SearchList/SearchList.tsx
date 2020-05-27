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
import { commonOp } from "@ducks/common";
import { IProjectOrTask, ItemDeleteModal } from "../modals/ItemDeleteModal";

export function SearchList() {
    const dispatch = useDispatch();

    const pageSizes = [10, 25, 50, 100];

    const data = useSelector(workspaceSel.resultsSelector);
    const pagination = useSelector(workspaceSel.paginationSelector);
    const appliedFilters = useSelector(workspaceSel.appliedFiltersSelector);
    const isLoading = useSelector(workspaceSel.isLoadingSelector);
    const appliedFacets = useSelector(workspaceSel.appliedFacetsSelector);
    const [deleteModalOpen, setDeleteModalOpen] = useState(false);
    const [selectedItem, setSelectedItem] = useState<IProjectOrTask | null>(null);
    const [showCloneModal, setShowCloneModal] = useState(false);

    const onDiscardModals = () => {
        setShowCloneModal(false);
        setSelectedItem(null);
    };

    const onOpenDuplicateModal = (item) => {
        setShowCloneModal(true);
        setSelectedItem(item);
    };

    const handleConfirmClone = (newId: string) => {
        const { id, projectId } = selectedItem;
        dispatch(workspaceOp.fetchCloneTaskAsync(id, projectId, newId));
        onDiscardModals();
    };

    const onOpenDeleteModal = (item: IProjectOrTask) => {
        setSelectedItem(item);
        setDeleteModalOpen(true);
    };

    const onCloseDeleteModal = () => {
        setSelectedItem(null);
        setDeleteModalOpen(false);
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
                textInfo={<p>No {appliedFilters.itemType} found.</p>}
                textCallout={<strong>Create your first {itemTypeLabel()} now.</strong>}
                actionButtons={[
                    <Button key={"create"} onClick={handleCreateArtefact} elevated>
                        Create {appliedFilters.itemType}
                    </Button>,
                ]}
            />
        ) : (
            <p>No Data Found</p>
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
                        <ItemDeleteModal selectedItem={selectedItem} onClose={onCloseDeleteModal} />
                    )}

                    <CloneModal
                        isOpen={showCloneModal}
                        oldId={selectedItem && selectedItem.id}
                        onDiscard={onDiscardModals}
                        onConfirm={handleConfirmClone}
                    />
                </>
            ) : null}
        </>
    );
}
