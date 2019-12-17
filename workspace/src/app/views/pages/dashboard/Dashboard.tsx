import React from "react";
import FilterBar from "./filter-bar/FilterBar";
import ProjectsList from "./projects-list/ProjectsList";
import './style.scss';

export default function DashboardLayout() {
    return (
        <div className='main clearfix'>
            <div className='left-content'>
                <FilterBar/>
            </div>
            <div className={'right-content'}>
                <ProjectsList />
            </div>
        </div>
    )
}
