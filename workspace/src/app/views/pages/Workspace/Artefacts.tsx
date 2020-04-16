import React, { useEffect } from 'react';
import { useDispatch, useSelector } from "react-redux";
import { workspaceOp, workspaceSel } from "@ducks/workspace";
import {
    WorkspaceContent,
    WorkspaceMain,
    WorkspaceSide,
    Section,
    SectionHeader,
    TitleMainsection,
    Grid,
    GridRow,
    GridColumn,
    Divider,
} from "@wrappers/index";
import Filterbar from "./Filterbar";
import SearchList from "./SearchList";
import SearchBar from "../../shared/SearchBar";
import { globalOp } from "@ducks/global";

const Artefacts = () => {
    const dispatch = useDispatch();

    const {textQuery} = useSelector(workspaceSel.appliedFiltersSelector);
    const sorters = useSelector(workspaceSel.sortersSelector);

    useEffect(() => {
        dispatch(globalOp.unsetProject());
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
                    <SectionHeader>
                        <Grid>
                            <GridRow>
                                <GridColumn small verticalAlign="center">
                                    <TitleMainsection>Contents</TitleMainsection>
                                </GridColumn>
                                <GridColumn full>
                                    <SearchBar
                                        textQuery={textQuery}
                                        sorters={sorters}
                                        onSort={handleSort}
                                        onApplyFilters={handleApplyFilter}
                                    />
                                </GridColumn>
                            </GridRow>
                        </Grid>
                    </SectionHeader>
                    <Divider addSpacing="medium"/>
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
                </Section>
            </WorkspaceSide>
        </WorkspaceContent>

    )
};

export default Artefacts;
