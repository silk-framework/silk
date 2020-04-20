import React, { useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { workspaceOp, workspaceSel } from "@ducks/workspace";
import { sharedOp } from "@ducks/shared";
import {
    Button,
    Icon,
    Spacing,
} from "@wrappers/index";
import Pagination from "../../../shared/Pagination";
import DataList from "../../../shared/Datalist";
import DeleteModal from "../../../shared/modals/DeleteModal";
import Loading from "../../../shared/Loading";
import CloneModal from "../../../shared/modals/CloneModal";
import { DATA_TYPES } from "../../../../constants";
import AppliedFacets from "../AppliedFacets";
import SearchItem from "./SearchItem";
import EmptyList from "./EmptyList";
import { globalOp } from "@ducks/common";

export function SearchList() {

    const dispatch = useDispatch();

    const pageSizes = [10, 25, 50, 100];

    const [deleteModalOptions, setDeleteModalOptions] = useState({});

    const data = useSelector(workspaceSel.resultsSelector);
    const pagination = useSelector(workspaceSel.paginationSelector);
    const appliedFilters = useSelector(workspaceSel.appliedFiltersSelector);
    const isLoading = useSelector(workspaceSel.isLoadingSelector);

    const [selectedItem, setSelectedItem] = useState();
    const [showDeleteModal, setShowDeleteModal] = useState();
    const [showCloneModal, setShowCloneModal] = useState();

    const onDiscardModals = () => {
        setShowDeleteModal(false);
        setShowCloneModal(false);
        setSelectedItem(null)
    };

    const onOpenDeleteModal = async (item?: any) => {
        setShowDeleteModal(true);
        setSelectedItem(item);
        setDeleteModalOptions({
            render: () => <Loading/>
        });

        try {
            const data = await sharedOp.getTaskMetadataAsync(item.id, item.projectId);
            const isProject = data.type !== DATA_TYPES.PROJECT;

            const {dependentTasksDirect} = data.relations;
            // Skip check the relations for projects
            if (!isProject && dependentTasksDirect.length) {
                setDeleteModalOptions({
                    confirmationRequired: true,
                    render: () =>
                        <div>
                            <p>There are tasks depending on task {item.label || item.id}. </p>
                            <p>Are you sure you want to delete all tasks below?</p>
                            <ul>
                                {
                                    dependentTasksDirect.map(rel => <li key={rel}>{rel}</li>)
                                }
                            </ul>
                        </div>
                });
            } else {
                setDeleteModalOptions({
                    confirmationRequired: false,
                    render: () => <p>
                        Are you sure you want to permanently remove this item?
                    </p>
                });
            }
        } catch {
            setDeleteModalOptions({
                confirmationRequired: false,
                render: () => <p>
                    Are you sure you want to permanently remove this item?
                </p>
            });
        }

    };

    const onOpenDuplicateModal = (item) => {
        setShowCloneModal(true);
        setSelectedItem(item);
    };

    const handleConfirmRemove = () => {
        const {id, projectId} = selectedItem;
        dispatch(workspaceOp.fetchRemoveTaskAsync(id, projectId));
        onDiscardModals();
    };

    const handleConfirmClone = (newId: string) => {
        const {id, projectId} = selectedItem;
        dispatch(workspaceOp.fetchCloneTaskAsync(id, projectId, newId));
        onDiscardModals();
    };

    const handlePaginationOnChange = (n: number, pageSize: number) => {
        dispatch(workspaceOp.changePageOp(n));
        dispatch(workspaceOp.changeLimitOp(pageSize));
    };

    const handleCreateArtefact = () => {
        dispatch(globalOp.setSelectedArtefactDType(appliedFilters.itemType))
    };

    const isEmpty = !isLoading && !data.length;

    return (
        <>
            <AppliedFacets/>
            <DataList
                isEmpty={isEmpty}
                isLoading={isLoading}
                hasSpacing
                emptyContainer={
                    <EmptyList
                        depiction={<Icon name={'artefact-'+appliedFilters.itemType} large />}
                        textInfo={<p>No {appliedFilters.itemType} found.</p>}
                        textCallout={<strong>Create your first {appliedFilters.itemType} now.</strong>}
                        actionButtons={[
                            <Button key={'create'} onClick={handleCreateArtefact} elevated>Create {appliedFilters.itemType}</Button>
                        ]}
                    />
                }
            >
                {
                    data.map(item => (
                        <SearchItem
                            key={`${item.id}_${item.projectId}`}
                            item={item}
                            onOpenDeleteModal={() => onOpenDeleteModal(item)}
                            onOpenDuplicateModal={() => onOpenDuplicateModal(item)}
                            searchValue={appliedFilters.textQuery}
                        />
                    ))
                }
            </DataList>
            {
                !isEmpty
                    ? <>
                        <Spacing size="small" />

                        <Pagination
                            pagination={pagination}
                            pageSizes={pageSizes}
                            onChangeSelect={handlePaginationOnChange}
                        />

                        <DeleteModal
                            isOpen={showDeleteModal}
                            onDiscard={onDiscardModals}
                            onConfirm={handleConfirmRemove}
                            {...deleteModalOptions}
                        />
                        <CloneModal
                            isOpen={showCloneModal}
                            oldId={selectedItem && selectedItem.id}
                            onDiscard={onDiscardModals}
                            onConfirm={handleConfirmClone}
                        />
                    </>
                    : null
            }
        </>
    )
}
