import React, { useEffect } from 'react';
import Main from "../../layout/Main";
import Filterbar from "./Filterbar";
import SearchList from "./SearchResults/SearchList";
import TopBar from "./Topbar";
import { workspaceOp } from "@ducks/workspace";
import { useDispatch } from "react-redux";

const Projects = () => {
    const dispatch = useDispatch();

    useEffect(() => {
        dispatch(workspaceOp.unsetProject());
        // Fetch the list of projects
        dispatch(workspaceOp.fetchListAsync());
    }, []);

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
