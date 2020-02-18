import React, { useEffect, useLayoutEffect } from "react";
import { globalOp } from "@ducks/global";
import { useDispatch } from "react-redux";
import Main from "../../layout/Main";
import Filterbar from "./Filterbar";
import Metadata from "../../components/Metadata";
import { workspaceOp } from "@ducks/workspace";
import SearchList from "./SearchResults/SearchList";
import TopBar from "./Topbar";
import ConfigurationWidget from "./widgets/Configuration";
import WarningWidget from "./widgets/Warning";
import FileWidget from "./widgets/File";

const ProjectDetails = ({projectId}) => {
    const dispatch = useDispatch();

    useLayoutEffect(() => {
        dispatch(workspaceOp.setProjectId(projectId));
    }, [projectId]);

    useEffect(() => {
        // Fetch the list of projects
        dispatch(workspaceOp.fetchListAsync());
        dispatch(globalOp.addBreadcrumb({
            href: `/projects/${projectId}`,
            text: projectId
        }));
    }, []);

    const {LeftPanel, RightPanel} = Main;

    return (
        <Main>
            <LeftPanel className='clearfix'>
                <Metadata taskId={projectId}/>
                <div className='filter-bar-content'>
                    <Filterbar/>
                </div>
                <div className='preview-content'>
                    <TopBar/>
                    <SearchList/>
                </div>
            </LeftPanel>
            <RightPanel>
                <FileWidget/>
                <ConfigurationWidget/>
                <WarningWidget/>
            </RightPanel>
        </Main>
    )
};

export default ProjectDetails;
