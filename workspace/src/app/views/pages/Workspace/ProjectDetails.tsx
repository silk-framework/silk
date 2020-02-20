import React, { useEffect, useLayoutEffect } from "react";
import { globalOp } from "@ducks/global";
import { useDispatch } from "react-redux";
import Main from "../../layout/Main";
import Filterbar from "./Filterbar";
import Metadata from "../../components/Metadata";
import { workspaceOp } from "@ducks/workspace";
import SearchList from "./SearchResults/SearchList";
import TopBar from "./Topbar";
import ConfigurationWidget from "./widgets/Configuration";
import WarningWidget from "./widgets/Warning";
import FileWidget from "./widgets/File";
import Grid from "@wrappers/carbon/grid";
import Row from "@wrappers/carbon/grid/Row";
import Col from "@wrappers/carbon/grid/Col";

const ProjectDetails = ({projectId}) => {
    const dispatch = useDispatch();

    useLayoutEffect(() => {
        dispatch(workspaceOp.setProjectId(projectId));
    }, [projectId]);

    useEffect(() => {
        // Fetch the list of projects
        dispatch(workspaceOp.fetchListAsync());
        dispatch(globalOp.addBreadcrumb({
            href: `/projects/${projectId}`,
            text: projectId
        }));
    }, []);

    return (
        <Main>
            <Grid>
                <Row>
                    <Col span={11}>
                        <Row><Metadata taskId={projectId}/></Row>
                        <Row>
                            <Col span={4} className='filter-bar-content'>
                                <Filterbar/>
                            </Col>
                            <Col span={12} className='preview-content'>
                                <TopBar/>
                                <SearchList/>
                            </Col>
                        </Row>
                    </Col>
                    <Col span={5}>
                        <FileWidget/>
                        <ConfigurationWidget/>
                        <WarningWidget/>
                    </Col>
                </Row>
            </Grid>
        </Main>
    )
};

export default ProjectDetails;
