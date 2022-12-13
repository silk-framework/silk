import { routerOp } from "@ducks/router";
import { requestProjectMetadata } from "@ducks/shared/requests";
import { importExampleProjectRequest } from "@ducks/workspace/requests";
import { MenuItem } from "@eccenca/gui-elements";
import React from "react";
import { useTranslation } from "react-i18next";
import { useDispatch } from "react-redux";

import useErrorHandler from "../../../hooks/useErrorHandler";

/** Component to load the "movies" example project. */
export const ExampleProjectImportMenu = () => {
    const { registerError } = useErrorHandler();
    const dispatch = useDispatch();
    const [t] = useTranslation();
    const [exampleProjectLoaded, setExampleProjectLoaded] = React.useState<boolean | undefined>(undefined);
    const [loading, setLoading] = React.useState(false);
    const moviesProjectId = "movies";

    React.useEffect(() => {
        checkMoviesProjectExists();
    }, []);

    const checkMoviesProjectExists = async () => {
        try {
            await requestProjectMetadata(moviesProjectId);
            setExampleProjectLoaded(true);
        } catch (ex) {
            setExampleProjectLoaded(false);
        }
    };

    const importExampleProject = async () => {
        try {
            setLoading(true);
            await importExampleProjectRequest();
            dispatch(routerOp.goToPage(`projects/${moviesProjectId}`));
        } catch (ex) {
            registerError("header.importExampleProject", "Could not import example project.", ex);
        } finally {
            setLoading(false);
        }
    };
    return exampleProjectLoaded !== null && !exampleProjectLoaded ? (
        <MenuItem
            text={t("common.action.loadExampleProject")}
            icon={"item-add-artefact"}
            disabled={loading}
            onClick={importExampleProject}
        />
    ) : null;
};
