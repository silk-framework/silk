import { commonOp } from "@ducks/common";
import { Button, Spacing, TitleMainsection } from "@eccenca/gui-elements";
import React from "react";
import { useTranslation } from "react-i18next";
import { useDispatch } from "react-redux";

import { DATA_TYPES } from "../../../../constants";

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
                {t(
                    "pages.workspace.empty",
                    "Your workspace is empty. In order to start, create a new project or import an existing project from an exported project file."
                )}
            </TitleMainsection>
            <Spacing size="large" />
            <Button data-test-id={"empty-workspace-create-project-btn"} onClick={openCreateProjectModal} large elevated>
                {t("pages.workspace.createProject", "Create Project")}
            </Button>
        </div>
    );
};
