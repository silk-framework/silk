import React, { useEffect, useLayoutEffect } from "react";
import { commonOp, commonSel } from "@ducks/common";
import { useDispatch, useSelector } from "react-redux";
import Filterbar from "../Workspace/Filterbar";
import Metadata from "../../shared/Metadata";
import { workspaceOp, workspaceSel } from "@ducks/workspace";
import SearchList from "../../shared/SearchList";
import ConfigurationWidget from "./ConfigWidget";
import WarningWidget from "./WarningWidget";
import FileWidget from "./FileWidget";
import Loading from "../../shared/Loading";

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
    Spacing,
    Divider,
} from "@wrappers/index";
import { SearchBar } from "../../shared/SearchBar/SearchBar";

const Project = ({projectId}) => {
    const dispatch = useDispatch();

    const currentProjectId = useSelector(commonSel.currentProjectIdSelector);
    const {textQuery} = useSelector(workspaceSel.appliedFiltersSelector);
    const sorters = useSelector(workspaceSel.sortersSelector);

    useEffect(() => {
        // Fetch the list of projects
        dispatch(commonOp.setProjectId(projectId));
    }, []);

    const handleSort = (sortBy: string) => {
        dispatch(workspaceOp.applySorterOp(sortBy));
    };

    const handleApplyFilter = (filters: any) => {
        dispatch(workspaceOp.applyFiltersOp(filters));
    };

    return (
        !currentProjectId ? <Loading /> :
        <WorkspaceContent className="eccapp-di__project">
            <WorkspaceMain>
                <Section>
                    <Metadata taskId={projectId}/>
                    <Spacing />
                </Section>
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
                    <Divider addSpacing="medium" />
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
                    <FileWidget/>
                    <Spacing />
                    <ConfigurationWidget/>
                    <Spacing />
                    <WarningWidget/>
                </Section>
            </WorkspaceSide>
        </WorkspaceContent>
    )
};

export default Project;
