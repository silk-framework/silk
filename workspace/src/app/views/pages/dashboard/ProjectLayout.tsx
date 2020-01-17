import React from 'react';
import Main from "../../layout/main/Main";
import FilterBar from "./filter-bar/FilterBar";
import ProjectsList from "./projects-list/ProjectsList";

const ProjectLayout = () => {
    return (
        <Main>
            <Main.LeftPanel className='clearfix'>
                <div className='filter-bar-content'>
                    <FilterBar/>
                </div>
                <div className='preview-content'>
                    <ProjectsList/>
                </div>
            </Main.LeftPanel>
        </Main>
    )
};

export default ProjectLayout;
