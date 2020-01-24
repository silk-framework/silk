import React from 'react';
import Main from "../../layout/Main";
import Index from "./Filterbar";
import SearchList from "./SearchResults/SearchList";
import TopBar from "./Topbar";

const Projects = () => {
    return (
        <Main>
            <Main.LeftPanel className='clearfix'>
                <div className='filter-bar-content'>
                    <Index/>
                </div>
                <div className='preview-content'>
                    <TopBar/>
                    <SearchList/>
                </div>
            </Main.LeftPanel>
        </Main>
    )
};

export default Projects;
