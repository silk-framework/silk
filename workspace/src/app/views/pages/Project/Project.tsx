import React, { useEffect, useLayoutEffect } from "react";
import { globalOp, globalSel } from "@ducks/global";
import { useDispatch, useSelector } from "react-redux";
import Filterbar from "../Workspace/Filterbar";
import Metadata from "../../shared/Metadata";
import { workspaceOp, workspaceSel } from "@ducks/workspace";
import SearchList from "../Workspace/SearchList";
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
    const currentProjectId = useSelector(globalSel.currentProjectIdSelector);

    useEffect(() => {
        // Fetch the list of projects
        dispatch(globalOp.setProjectId(projectId));
        dispatch(globalOp.fetchArtefactsListAsync());
    }, []);

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
                                    <SearchBar onSort={() => {}} onApplyFilters={() => {}} />
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
