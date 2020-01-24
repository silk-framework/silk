import React, { useEffect, useLayoutEffect } from "react";
import { globalOp } from "@ducks/global";
import { useDispatch, useSelector } from "react-redux";
import Main from "../../layout/main/Main";
import FilterBar from "./filterbar/FilterBar";
import Metadata from "../../components/metadata/Metadata";
import { dashboardOp, dashboardSel } from "@ducks/dashboard";
import ProjectList from "./project/ProjectList";
import TopBar from "./topbar/TopBar";

const ProjectDetails = ({ projectId }) => {
    const dispatch = useDispatch();
    const projectMetadata = useSelector(dashboardSel.projectMetadataSelector);

    useLayoutEffect(() => {
        dispatch(dashboardOp.setProjectId(projectId));
    }, [projectId]);

    useEffect(() => {
        dispatch(dashboardOp.fetchProjectMetadata());
        dispatch(globalOp.addBreadcrumb({
            href: `/project/${projectId}`,
            text: projectId
        }));
    }, []);

    return (
        <Main>
            <Main.LeftPanel className='clearfix'>
                <Metadata metadata={projectMetadata}/>
                <div className='filter-bar-content'>
                    <FilterBar/>
                </div>
                <div className='preview-content'>
                    <TopBar/>
                    <ProjectList/>
                </div>
            </Main.LeftPanel>
        </Main>
    )
};

export default ProjectDetails;
