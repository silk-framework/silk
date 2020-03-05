import React, { useEffect } from 'react';
import Filterbar from "./Filterbar";
import SearchList from "./SearchResults/SearchList";
import TopBar from "./Topbar";
import { workspaceOp } from "@ducks/workspace";
import { useDispatch } from "react-redux";
import EmptyWorkspace from "./EmptyWorkspace";

import {
    WorkspaceContent,
    WorkspaceMain,
    WorkspaceSide,
    WorkspaceSection,
    WorkspaceGrid,
    WorkspaceRow,
    WorkspaceColumn,
} from "@wrappers/index";

const Projects = () => {
    const dispatch = useDispatch();

    useEffect(() => {
        dispatch(workspaceOp.unsetProject());
        // Fetch the list of projects
        dispatch(workspaceOp.fetchListAsync());
    }, []);

    return (
        <WorkspaceContent className="eccapp-di__workspace">
            <WorkspaceMain>
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
                    <EmptyWorkspace />
                </WorkspaceSection>
            </WorkspaceSide>
        </WorkspaceContent>
    )
};

export default Projects;
