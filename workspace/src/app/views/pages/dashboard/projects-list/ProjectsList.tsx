import React, { useEffect, useState } from "react";
import Pagination from "../../../components/pagination/Pagination";
import { useDispatch, useSelector } from "react-redux";
import { dashboardOp, dashboardSel } from "@ducks/dashboard";
import ActionsTopBar from "./ActionsTopBar";
import AppliedFacets from "./AppliedFacets";
import DataList from "../../../components/datalist/DataList";
import DeleteModal from "../../../components/modals/DeleteModal";
import ProjectRow from "./ProjectRow";
import Loading from "../../../components/loading/Loading";
import { push } from "connected-react-router";
import { ISearchResultsTask } from "@ducks/dashboard/typings";
import { DATA_TYPES } from "../../../../constants";
import CloneModal from "../../../components/modals/CloneModal";
import { routerSel } from "@ducks/router";

export default function ProjectsList() {

    const dispatch = useDispatch();

    const [deleteModalOptions, setDeleteModalOptions] = useState({});

    const data = useSelector(dashboardSel.resultsSelector);
    const sorters = useSelector(dashboardSel.sortersSelector);
    const pagination = useSelector(dashboardSel.paginationSelector);
    const appliedFilters = useSelector(dashboardSel.appliedFiltersSelector);
    const isLoading = useSelector(dashboardSel.isLoadingSelector);
    const pathname = useSelector(routerSel.pathnameSelector);
    const qs = useSelector(routerSel.routerSearchSelector);

    const [selectedItem, setSelectedItem] = useState();
    const [showDeleteModal, setShowDeleteModal] = useState();
    const [showCloneModal, setShowCloneModal] = useState();

    useEffect(() => {
        // Setup the filters from query string
        dispatch(dashboardOp.setupFiltersFromQs(qs));
    }, []);

    useEffect(() => {
        dispatch(dashboardOp.fetchListAsync());
    }, [appliedFilters, sorters.applied, pagination.current]);

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

        const data = await dashboardOp.getTaskMetadataAsync(item.id, item.projectId);
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
        if (item.type === DATA_TYPES.PROJECT) {
            dispatch(
                push(`${pathname}/project/${item.id}`)
            );
        }
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
                    data.map(item => <ProjectRow
                        key={item.id}
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
