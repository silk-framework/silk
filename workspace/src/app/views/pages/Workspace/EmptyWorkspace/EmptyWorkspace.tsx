import React, { useState } from 'react';
import { Button, Card, CardActions, CardContent, CardHeader, CardTitle } from '@wrappers/index';
import { useDispatch } from "react-redux";
import { globalOp } from "@ducks/common";

export const EmptyWorkspace = () => {
    const dispatch = useDispatch();

    const openCreateProjectModal = () => {
        dispatch(
            globalOp.selectArtefact({
                key: 'project'
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
