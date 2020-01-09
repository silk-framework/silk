import React, { useEffect } from "react";
import FilterBar from "./filter-bar/FilterBar";
import ProjectsList from "./projects-list/ProjectsList";

import './style.scss';
import { useSelector } from "react-redux";
import { dashboardSel } from "@ducks/dashboard";
import { AppToaster } from "../../../services/toaster";
import { Intent } from "@wrappers/constants";

export default function Dashboard() {
    const error = useSelector(dashboardSel.errorSelector);
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
                <ProjectsList/>
            </div>
        </div>
    )
}
