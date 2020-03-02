import React, { useEffect } from 'react';
import Main from "../../layout/Main";
import Filterbar from "./Filterbar";
import SearchList from "./SearchResults/SearchList";
import TopBar from "./Topbar";
import { workspaceOp } from "@ducks/workspace";
import { useDispatch } from "react-redux";
import Grid from "@wrappers/carbon/grid";
import Row from "@wrappers/carbon/grid/Row";
import Col from "@wrappers/carbon/grid/Col";
import EmptyWorkspace from "./EmptyWorkspace";

const Projects = () => {
    const dispatch = useDispatch();

    useEffect(() => {
        dispatch(workspaceOp.unsetProject());
        // Fetch the list of projects
        dispatch(workspaceOp.fetchListAsync());
    }, []);

    return (
        <Main>
            <Grid>
                <Row>
                    <Col span={12}>
                        <EmptyWorkspace />
                    </Col>
                </Row>
                <Row>
                    <Col span={2} className='filter-bar-content'>
                        <Filterbar/>
                    </Col>
                    <Col span={6} className='preview-content'>
                        <TopBar/>
                        <SearchList/>
                    </Col>
                </Row>
            </Grid>
        </Main>
    )
};

export default Projects;
