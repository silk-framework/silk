import React, { useEffect, useLayoutEffect } from "react";
import { globalOp, globalSel } from "@ducks/global";
import { useDispatch, useSelector } from "react-redux";
import Filterbar from "./Filterbar";
import Metadata from "../../components/Metadata";
import { workspaceOp, workspaceSel } from "@ducks/workspace";
import SearchList from "./SearchList";
import TopBar from "./AppliedFacets";
import ConfigurationWidget from "./widgets/Configuration";
import WarningWidget from "./widgets/Warning";
import FileWidget from "./widgets/File";
import Loading from "../../components/Loading";

import {
    WorkspaceContent,
    WorkspaceMain,
    WorkspaceSide,
    WorkspaceSection,
    WorkspaceGrid,
    WorkspaceRow,
    WorkspaceColumn,
    Spacing,
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
                <WorkspaceSection>
                    <Metadata taskId={projectId}/>
                    <Spacing />
                </WorkspaceSection>
                <WorkspaceSection>
                    <TopBar/>
                    <WorkspaceGrid>
                        <WorkspaceRow>
                            <WorkspaceColumn small>
                                <Filterbar/>
                            </WorkspaceColumn>
                            <WorkspaceColumn full>
                                <SearchList/>
                            </WorkspaceColumn>
                        </WorkspaceRow>
                    </WorkspaceGrid>
                </WorkspaceSection>
            </WorkspaceMain>
            <WorkspaceSide>
                <WorkspaceSection>
                    <FileWidget/>
                    <Spacing />
                    <ConfigurationWidget/>
                    <Spacing />
                    <WarningWidget/>
                </WorkspaceSection>
            </WorkspaceSide>
        </WorkspaceContent>
    )
};

export default ProjectDetails;
