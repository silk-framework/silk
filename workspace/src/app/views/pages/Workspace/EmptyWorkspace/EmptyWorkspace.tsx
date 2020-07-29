import React from "react";
import { Button, Spacing, TitleMainsection } from "@gui-elements/index";
import { useDispatch } from "react-redux";
import { commonOp } from "@ducks/common";
import { DATA_TYPES } from "../../../../constants";

export const EmptyWorkspace = () => {
    const dispatch = useDispatch();

    const openCreateProjectModal = () => {
        dispatch(
            commonOp.selectArtefact({
                key: DATA_TYPES.PROJECT,
            })
        );
    };

    return (
        <div style={{ textAlign: "center" }}>
            <TitleMainsection>Workspace is empty, so start please create your first project</TitleMainsection>
            <Spacing size="large" />
            <Button onClick={openCreateProjectModal} large elevated>
                Create Project
            </Button>
        </div>
    );
};
