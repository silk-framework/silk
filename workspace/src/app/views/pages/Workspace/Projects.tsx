import React from 'react';
import Main from "../../layout/Main";
import Filterbar from "./Filterbar";
import SearchList from "./SearchResults/SearchList";
import TopBar from "./Topbar";

const Projects = () => {
    return (
        <Main>
            <Main.LeftPanel className='clearfix'>
                <div className='filter-bar-content'>
                    <Filterbar/>
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
