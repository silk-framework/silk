import React, { useEffect, useLayoutEffect } from "react";
import { globalOp, globalSel } from "@ducks/global";
import { useDispatch, useSelector } from "react-redux";
import Filterbar from "./Filterbar";
import Metadata from "../../components/Metadata";
import { workspaceOp, workspaceSel } from "@ducks/workspace";
import SearchList from "./SearchList";
import ConfigurationWidget from "./widgets/Configuration";
import WarningWidget from "./widgets/Warning";
import FileWidget from "./widgets/File";
import Loading from "../../components/Loading";

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

const ProjectDetails = ({projectId}) => {
    const dispatch = useDispatch();
    const currentProjectId = useSelector(globalSel.currentProjectIdSelector);

    useEffect(() => {
        // Fetch the list of projects
        dispatch(globalOp.setProjectId(projectId));
        dispatch(globalOp.fetchArtefactsListAsync());

        dispatch(workspaceOp.fetchListAsync());
        dispatch(globalOp.addBreadcrumb({
            href: `/projects/${projectId}`,
            text: projectId
        }));
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
                                    todo: search bar here
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

export default ProjectDetails;
