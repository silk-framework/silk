import React, { useEffect } from "react";
import ProjectsList from "./projects-list/ProjectsList";

import './style.scss';
import { useDispatch, useSelector } from "react-redux";
import { dashboardSel } from "@ducks/dashboard";
import { AppToaster } from "../../../services/toaster";
import { Intent } from "@wrappers/constants";
import { globalOp } from "@ducks/global";
import FilterBar from "./filter-bar/FilterBar";
import { useParams } from "react-router";
import TasksList from "./tasks-list/TasksList";

export default function Dashboard() {
    const dispatch = useDispatch();
    const error = useSelector(dashboardSel.errorSelector);

    const {projectId} = useParams();

    useEffect(() => {
        if (projectId) {
            dispatch(globalOp.addBreadcrumb({
                href: `/project/${projectId}`,
                text: projectId
            }));
        }
    }, []);

    // useEffect(() => {
    //     if (selectedProject !== projectId) {
    //         // clear filter on page change
    //         dispatch(dashboardOp.resetFilters());
    //     }
    //
    //     if (selectedProject && !projectId) {
    //         dispatch(dashboardOp.unsetProject())
    //     }
    // }, [projectId]);

    useEffect(() => {
        if (error.detail) {
            AppToaster.show({
                message: error.detail,
                intent: Intent.DANGER,
                timeout: 2000
            })
        }
    }, [error.detail]);

    return (
        <div className='main clearfix'>
            <div className='left-content'>
                <FilterBar/>
            </div>
            <div className='right-content'>
                {
                    projectId
                        ? <TasksList/>
                        : <ProjectsList/>
                }
            </div>
        </div>
    )
}
