import React, { useEffect } from "react";
import ProjectsList from "./projects-list/ProjectsList";

import './style.scss';
import { useDispatch, useSelector } from "react-redux";
import { dashboardSel } from "@ducks/dashboard";
import { AppToaster } from "../../../services/toaster";
import { Intent } from "@wrappers/constants";
import { globalOp } from "@ducks/global";
import FilterBar from "./filter-bar/FilterBar";

export default function Dashboard() {
    const dispatch = useDispatch();
    useEffect(() => {
        dispatch(globalOp.addBreadcrumb({
            href: '',
            text: 'Home'
        }));
        dispatch(globalOp.fetchAvailableDTypesAsync());
    }, []);

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
