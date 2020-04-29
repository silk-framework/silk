import React, { useEffect } from "react";
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
    Divider,
    Grid,
    GridColumn,
    GridRow,
    Notification,
    Section,
    SectionHeader,
    Spacing,
    TitleMainsection,
    WorkspaceContent,
    WorkspaceMain,
    WorkspaceSide,
} from "@wrappers/index";
import { SearchBar } from "../../shared/SearchBar/SearchBar";

const Project = ({ projectId }) => {
    const dispatch = useDispatch();

    const currentProjectId = useSelector(commonSel.currentProjectIdSelector);
    const { textQuery } = useSelector(workspaceSel.appliedFiltersSelector);
    const sorters = useSelector(workspaceSel.sortersSelector);
    const error = useSelector(workspaceSel.errorSelector);

    useEffect(() => {
        // Fetch the list of projects
        dispatch(commonOp.setProjectId(projectId));
    }, [projectId]);

    const handleSort = (sortBy: string) => {
        dispatch(workspaceOp.applySorterOp(sortBy));
    };

    const handleSearch = (textQuery: string) => {
        dispatch(workspaceOp.applyFiltersOp({ textQuery }));
    };

    return !currentProjectId ? (
        <Loading />
    ) : (
        <WorkspaceContent className="eccapp-di__project">
            <WorkspaceMain>
                <Section>
                    <Metadata taskId={projectId} />
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
                <Section>
                    <FileWidget />
                    <Spacing />
                    <ConfigurationWidget />
                    <Spacing />
                    <WarningWidget />
                </Section>
            </WorkspaceSide>
        </WorkspaceContent>
    );
};

export default Project;
