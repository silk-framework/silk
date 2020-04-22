import React from 'react';
import { Button, Card, CardActions, CardHeader, CardTitle } from '@wrappers/index';
import { useDispatch } from "react-redux";
import { commonOp } from "@ducks/common";
import { DATA_TYPES } from "../../../../constants";

export const EmptyWorkspace = () => {
    const dispatch = useDispatch();

    const openCreateProjectModal = () => {
        dispatch(
            commonOp.selectArtefact({
                key: DATA_TYPES.PROJECT
            })
        );
    };

    return <>
        <Card style={{margin: '20px auto'}}>
            <div>
                <CardHeader>
                    <CardTitle>Workspace is empty, so start please create your first project</CardTitle>
                </CardHeader>
                <CardActions>
                    <Button onClick={openCreateProjectModal}>Create Project</Button>
                </CardActions>
            </div>
        </Card>
    </>
};
