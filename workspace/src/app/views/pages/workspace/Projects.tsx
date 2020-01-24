import React from 'react';
import Main from "../../layout/main/Main";
import FilterBar from "./filterbar/FilterBar";
import ProjectList from "./project/ProjectList";
import TopBar from "./topbar/TopBar";

const Projects = () => {
    return (
        <Main>
            <Main.LeftPanel className='clearfix'>
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

export default Projects;
