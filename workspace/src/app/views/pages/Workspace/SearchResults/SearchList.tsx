import React, { useState } from "react";
import Pagination from "../../../components/Pagination";
import { useDispatch, useSelector } from "react-redux";
import { workspaceOp, workspaceSel } from "@ducks/workspace";
import AppliedFacets from "../Topbar/AppliedFacets";
import DataList from "../../../components/Datalist";
import DeleteModal from "../../../components/modals/DeleteModal";
import SearchItem from "./SearchItem";
import Loading from "../../../components/Loading";
import { DATA_TYPES } from "../../../../constants";
import CloneModal from "../../../components/modals/CloneModal";
import { sharedOp } from "@ducks/shared";

export default function SearchList() {

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

    const {Header, Body, Footer} = DataList;
    return (
        <DataList isLoading={isLoading} data={data}>
            <Body className={'cardBody'}>
                <AppliedFacets/>
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
            </Body>
            <Footer>
                <Pagination
                    pagination={pagination}
                    pageSizes={pageSizes}
                    onChangeSelect={handlePaginationOnChange}
                />
            </Footer>
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
        </DataList>
    )
}
