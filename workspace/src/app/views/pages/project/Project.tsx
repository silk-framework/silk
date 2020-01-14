import React, { useEffect } from "react";
import { useParams } from "react-router";

import './style.scss';
import { projectOp } from "@ducks/project";
import { useDispatch } from "react-redux";
import { globalOp } from "@ducks/global";
import FilterBar from "./filter-bar/FilterBar";

export default function Project() {
    const {projectId} = useParams();
    const dispatch = useDispatch();

    useEffect(() => {
        dispatch(globalOp.fetchAvailableDTypesAsync(projectId));
        dispatch(projectOp.setProjectAsync(projectId));
        dispatch(globalOp.addBreadcrumb({
            href: `/project/${projectId}`,
            text: projectId
        }));
    }, []);

    return (
        <div className='main clearfix'>
            <div className='left-content'>
                <FilterBar/>
            </div>
            <div className='mid-content'>

            </div>
            <div className='right-content'>
            </div>
        </div>
    )
}
