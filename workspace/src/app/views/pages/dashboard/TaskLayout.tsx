import React, { useEffect } from "react";
import { globalOp } from "@ducks/global";
import { useDispatch, useSelector } from "react-redux";
import Main from "../../layout/main/Main";
import TasksList from "./tasks-list/TasksList";
import FilterBar from "./filter-bar/FilterBar";
import Metadata from "../../components/metadata/Metadata";
import { dashboardSel } from "@ducks/dashboard";

const TaskLayout = ({ projectId }) => {
    const dispatch = useDispatch();
    const projectMetadata = useSelector(dashboardSel.projectMetadataSelector);

    useEffect(() => {
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
                    <TasksList/>
                </div>
            </Main.LeftPanel>
        </Main>
    )
};

export default TaskLayout;
