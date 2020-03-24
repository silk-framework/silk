import React, { useState } from 'react';
import {Button} from '@wrappers/index';
import { useDispatch } from "react-redux";
import { globalOp } from "@ducks/global";

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
        <div>
            <p>Workspace is empty, so start please create your first project</p>
            <Button onClick={openCreateProjectModal} large>Create Project</Button>
        </div>
    </>
};
