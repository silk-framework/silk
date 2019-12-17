import React, { useEffect, useState } from "react";
import Pagination from "./components/Pagination";
import { useDispatch, useSelector } from "react-redux";
import { dashboardOp, dashboardSel } from "../../../../state/ducks/dashboard";
import ActionsTopBar from "./ActionsTopBar";
import AppliedFacets from "./components/AppliedFacets";
import DataList from "../../../components/datalist/DataList";
import DeleteModal from "../../../components/modals/DeleteModal";
import ProjectRow from "./ProjectRow";
import Loading from "../../../components/loading/Loading";

import './style.scss';

export default function ProjectsList() {

    const dispatch = useDispatch();

    const [deleteModalOptions, setDeleteModalOptions] = useState({});

    const data = useSelector(dashboardSel.resultsSelector);
    const facets = useSelector(dashboardSel.facetsSelector);
    const sorters = useSelector(dashboardSel.sortersSelector);

    const pagination = useSelector(dashboardSel.paginationSelector);
    const appliedFilters = useSelector(dashboardSel.appliedFiltersSelector);
    const appliedFacets = useSelector(dashboardSel.appliedFacetsSelector);

    const [selectedItem, setSelectedItem] = useState();
    const [showDeleteModal, setShowDeleteModal] = useState();

    useEffect(() => {
        dispatch(dashboardOp.fetchListAsync());
    }, [appliedFilters, sorters.applied, pagination.current]);

    const onDiscardDeleteModal = () => {
        setShowDeleteModal(false);
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

    const handleConfirmRemove = () => {
        setShowDeleteModal(false);
        dispatch(dashboardOp.fetchRemoveTaskAsync(selectedItem.id, selectedItem.projectId));
    };

    const handlePageChange = (n: number) => {
        dispatch(dashboardOp.changePage(n))
    };

    const handleFacetRemove = (facetId: string, keywordId: string) => {
        dispatch(dashboardOp.removeFacet({
            facetId,
            keywordId
        }));
    };

    const facetsList = [];
    appliedFacets.map(appliedFacet => {
        const facet = facets.find(o => o.id === appliedFacet.facetId);
        if (facet) {
            facetsList.push({
                label: facet.label,
                id: facet.id,
                keywords: facet.values.filter(key => appliedFacet.keywordIds.includes(key.id))
            });

        }
    });

    const {Header, Body, Footer} = DataList;
    return (
        <>
            <ActionsTopBar/>
            {
                !data.length ? <p>No resources found</p> :
                    <DataList>
                        <Header>
                            <AppliedFacets
                                facetsList={facetsList}
                                onFacetRemove={handleFacetRemove}
                            />
                        </Header>
                        <Body>
                        {
                            data.map(item => <ProjectRow
                                key={item.id}
                                item={item}
                                onOpenDeleteModal={onOpenDeleteModal}
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
            }
            <DeleteModal
                isOpen={showDeleteModal}
                onDiscard={onDiscardDeleteModal}
                onConfirm={handleConfirmRemove}
                {...deleteModalOptions}
            />
        </>
    )
}
