import React, { useEffect } from 'react';
import Filterbar from "./Filterbar";
import SearchList from "./SearchList";
import { workspaceOp, workspaceSel } from "@ducks/workspace";
import { useDispatch, useSelector } from "react-redux";
import EmptyWorkspace from "./EmptyWorkspace";

import {
    WorkspaceContent,
    WorkspaceMain,
    WorkspaceSide,
    Section,
    Grid,
    GridRow,
    GridColumn,
} from "@wrappers/index";
import SearchBar from "../../components/SearchBar";

const Projects = () => {
    const dispatch = useDispatch();
    const { textQuery } = useSelector(workspaceSel.appliedFiltersSelector);
    const sorters = useSelector(workspaceSel.sortersSelector);

    useEffect(() => {
        dispatch(workspaceOp.unsetProject());
        // Fetch the list of projects
        dispatch(workspaceOp.fetchListAsync());
    }, []);

    const handleSort = (sortBy: string) => {
        dispatch(workspaceOp.applySorterOp(sortBy));
    };

    const handleApplyFilter = (filters: any) => {
        dispatch(workspaceOp.applyFiltersOp(filters));
    };

    return (
        <WorkspaceContent className="eccapp-di__workspace">
            <WorkspaceMain>
                <Section>
                    <SearchBar
                        textQuery={textQuery}
                        sorters={sorters}
                        onSort={handleSort}
                        onApplyFilters={handleApplyFilter}
                    />
                    <Grid>
                        <GridRow>
                            <GridColumn small>
                                <Filterbar/>
                            </GridColumn>
                            <GridColumn full>
                                <SearchList/>
                            </GridColumn>
                        </GridRow>
                    </Grid>
                </Section>
            </WorkspaceMain>
            <WorkspaceSide>
                <Section>
                    <EmptyWorkspace />
                </Section>
            </WorkspaceSide>
        </WorkspaceContent>
    )
};

export default Projects;
