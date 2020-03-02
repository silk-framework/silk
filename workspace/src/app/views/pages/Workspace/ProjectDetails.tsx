import React, { useEffect, useLayoutEffect } from "react";
import { globalOp } from "@ducks/global";
import { useDispatch, useSelector } from "react-redux";
import Main from "../../layout/Main";
import Filterbar from "./Filterbar";
import Metadata from "../../components/Metadata";
import { workspaceOp, workspaceSel } from "@ducks/workspace";
import SearchList from "./SearchResults/SearchList";
import TopBar from "./Topbar";
import ConfigurationWidget from "./widgets/Configuration";
import WarningWidget from "./widgets/Warning";
import FileWidget from "./widgets/File";
import Grid from "@wrappers/carbon/grid";
import Row from "@wrappers/carbon/grid/Row";
import Col from "@wrappers/carbon/grid/Col";
import Loading from "../../components/Loading";

const ProjectDetails = ({projectId}) => {
    const dispatch = useDispatch();
    const currentProjectId = useSelector(workspaceSel.currentProjectIdSelector);

    useEffect(() => {
        // Fetch the list of projects
        dispatch(workspaceOp.setProjectId(projectId));
        dispatch(workspaceOp.fetchListAsync());
        dispatch(globalOp.addBreadcrumb({
            href: `/projects/${projectId}`,
            text: projectId
        }));
    }, []);

    return (
        !currentProjectId ? <Loading /> :
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
