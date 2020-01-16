import React, { useEffect, useState } from "react";
import Pagination from "../../../components/pagination/Pagination";
import { useDispatch, useSelector } from "react-redux";
import { dashboardOp, dashboardSel } from "@ducks/dashboard";
import DataList from "../../../components/datalist/DataList";
import DeleteModal from "../../../components/modals/DeleteModal";
import TaskRow from "./TaskRow";
import Loading from "../../../components/loading/Loading";
import { ISearchResultsTask } from "@ducks/dashboard/typings";
import CloneModal from "../../../components/modals/CloneModal";
import { useParams } from "react-router";
import ActionsTopBar from "../topbar/ActionsTopBar";
import AppliedFacets from "../topbar/AppliedFacets";

export default function TasksList() {

    const dispatch = useDispatch();

    const [deleteModalOptions, setDeleteModalOptions] = useState({});

    const data = useSelector(dashboardSel.resultsSelector);
    const pagination = useSelector(dashboardSel.paginationSelector);
    const appliedFilters = useSelector(dashboardSel.appliedFiltersSelector);
    const isLoading = useSelector(dashboardSel.isLoadingSelector);

    const [selectedItem, setSelectedItem] = useState();
    const [showDeleteModal, setShowDeleteModal] = useState();
    const [showCloneModal, setShowCloneModal] = useState();

    const {projectId} = useParams();

    useEffect(() => {
        dispatch(dashboardOp.setProjectId(projectId));
        dispatch(dashboardOp.fetchProjectMetadata());
        // Fetch the list of tasks
        dispatch(dashboardOp.fetchListAsync());
    }, []);

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
            const data = await dashboardOp.getTaskMetadataAsync(item.id, projectId);
            const {dependentTasksDirect} = data.relations;

            if (dependentTasksDirect.length) {
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
        const { id, projectId } = selectedItem;
        dispatch(dashboardOp.fetchRemoveTaskAsync(id, projectId));
        onDiscardModals();
    };

    const handleConfirmClone = (newId: string) => {
        const { id, projectId } = selectedItem;
        dispatch(dashboardOp.fetchCloneTaskAsync(id, projectId, newId));
        onDiscardModals();
    };

    const handlePageChange = (n: number) => {
        dispatch(dashboardOp.changePageOp(n))
    };

    const goToItemDetails = (item: ISearchResultsTask) => {
        // if (item.type === DATA_TYPES.PROJECT) {
        //     dispatch(
        //         push(`${pathname}/project/${item.id}`)
        //     );
        // }
    };

    const {Header, Body, Footer} = DataList;
    return (
        <>
            <ActionsTopBar/>
            <DataList isLoading={isLoading} data={data}>
                <Header>
                    <AppliedFacets/>
                </Header>
                <Body>
                {
                    data.map(item => <TaskRow
                        key={`${item.id}_${item.projectId}`}
                        item={item}
                        onOpenDeleteModal={() => onOpenDeleteModal(item)}
                        onOpenDuplicateModal={() => onOpenDuplicateModal(item)}
                        onRowClick={() => goToItemDetails(item)}
                        searchValue={appliedFilters.textQuery}
                    />)
                }
                </Body>
                <Footer>
                    <Pagination
                        pagination={pagination}
                        onPageChange={handlePageChange}
                    />
                </Footer>
            </DataList>
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
    )
}
