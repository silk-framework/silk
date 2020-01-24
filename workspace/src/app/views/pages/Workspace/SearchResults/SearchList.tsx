import React, { useEffect, useState } from "react";
import Pagination from "../../../components/Pagination";
import { useDispatch, useSelector } from "react-redux";
import { workspaceOp, workspaceSel } from "@ducks/workspace";
import TopBar from "../Topbar";
import AppliedFacets from "../Topbar/AppliedFacets";
import DataList from "../../../components/Datalist";
import DeleteModal from "../../../components/modals/DeleteModal";
import SearchItem from "./SearchItem";
import Loading from "../../../components/Loading";
import { push } from "connected-react-router";
import { ISearchResultsTask } from "@ducks/workspace/typings";
import { DATA_TYPES } from "../../../../constants";
import CloneModal from "../../../components/modals/CloneModal";
import { routerSel } from "@ducks/router";

export default function SearchList() {

    const dispatch = useDispatch();

    const [deleteModalOptions, setDeleteModalOptions] = useState({});

    const data = useSelector(workspaceSel.resultsSelector);
    const pagination = useSelector(workspaceSel.paginationSelector);
    const appliedFilters = useSelector(workspaceSel.appliedFiltersSelector);
    const isLoading = useSelector(workspaceSel.isLoadingSelector);
    const pathname = useSelector(routerSel.pathnameSelector);
    const qs = useSelector(routerSel.routerSearchSelector);

    const [selectedItem, setSelectedItem] = useState();
    const [showDeleteModal, setShowDeleteModal] = useState();
    const [showCloneModal, setShowCloneModal] = useState();

    useEffect(() => {
        // Fetch the list of projects
        dispatch(workspaceOp.fetchListAsync());
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
            const data = await workspaceOp.getTaskMetadataAsync(item.id, item.projectId);
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
        dispatch(workspaceOp.fetchRemoveTaskAsync(id, projectId));
        onDiscardModals();
    };

    const handleConfirmClone = (newId: string) => {
        const { id, projectId } = selectedItem;
        dispatch(workspaceOp.fetchCloneTaskAsync(id, projectId, newId));
        onDiscardModals();
    };

    const handlePageChange = (n: number) => {
        dispatch(workspaceOp.changePageOp(n))
    };

    const goToItemDetails = (item: ISearchResultsTask) => {
        if (item.type === DATA_TYPES.PROJECT) {
            dispatch(
                push(`${pathname}/projects/${item.id}`)
            );
        }
        if (item.type === DATA_TYPES.DATASET) {
            dispatch(
                push(`${pathname}/datasets/${item.id}`)
            );
        }
    };

    const {Header, Body, Footer} = DataList;
    return (
        <>
            <DataList isLoading={isLoading} data={data}>
                <Header>
                    <AppliedFacets/>
                </Header>
                <Body>
                {
                    data.map(item => <SearchItem
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
