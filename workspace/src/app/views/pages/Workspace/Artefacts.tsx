import React, { useEffect } from "react";
import { useDispatch, useSelector } from "react-redux";
import { workspaceOp, workspaceSel } from "@ducks/workspace";
import {
    Divider,
    Grid,
    GridColumn,
    GridRow,
    Notification,
    Section,
    SectionHeader,
    TitleMainsection,
    WorkspaceContent,
    WorkspaceMain,
    WorkspaceSide,
} from "@wrappers/index";
import Filterbar from "./Filterbar";
import SearchList from "../../shared/SearchList";
import SearchBar from "../../shared/SearchBar";
import { commonOp } from "@ducks/common";

const Artefacts = () => {
    const dispatch = useDispatch();

    const { textQuery } = useSelector(workspaceSel.appliedFiltersSelector);
    const sorters = useSelector(workspaceSel.sortersSelector);
    const error = useSelector(workspaceSel.errorSelector);

    useEffect(() => {
        dispatch(commonOp.unsetProject());
    }, []);

    const handleSort = (sortBy: string) => {
        dispatch(workspaceOp.applySorterOp(sortBy));
    };

    const handleSearch = (textQuery: string) => {
        dispatch(workspaceOp.applyFiltersOp({ textQuery }));
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
                                        onSearch={handleSearch}
                                    />
                                </GridColumn>
                            </GridRow>
                        </Grid>
                    </SectionHeader>
                    <Divider addSpacing="medium" />
                    <Grid>
                        <GridRow>
                            <GridColumn small>
                                <Filterbar />
                            </GridColumn>
                            <GridColumn full>
                                {error.detail ? (
                                    <Notification danger>
                                        <h3>Error, cannot fetch results.</h3>
                                        <p>{error.detail}</p>
                                    </Notification>
                                ) : (
                                    <SearchList />
                                )}
                            </GridColumn>
                        </GridRow>
                    </Grid>
                </Section>
            </WorkspaceMain>
            <WorkspaceSide>
                <Section></Section>
            </WorkspaceSide>
        </WorkspaceContent>
    );
};

export default Artefacts;
