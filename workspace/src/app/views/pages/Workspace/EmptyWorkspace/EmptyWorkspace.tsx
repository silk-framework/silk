import React from "react";
import { Button, Spacing, TitleMainsection } from "@wrappers/index";
import { useDispatch } from "react-redux";
import { commonOp } from "@ducks/common";
import { DATA_TYPES } from "../../../../constants";
import { useTranslation } from "react-i18next";

export const EmptyWorkspace = () => {
    const dispatch = useDispatch();
    const [t] = useTranslation();
    const openCreateProjectModal = () => {
        dispatch(
            commonOp.selectArtefact({
                key: DATA_TYPES.PROJECT,
            })
        );
    };

    return (
        <div style={{ textAlign: "center" }}>
            <TitleMainsection>
                {t("pages.workspace.empty", "Workspace is empty, so start please create your first project")}
            </TitleMainsection>
            <Spacing size="large" />
            <Button onClick={openCreateProjectModal} large elevated>
                {t("pages.workspace.createProject", "Create Project")}
            </Button>
        </div>
    );
};
