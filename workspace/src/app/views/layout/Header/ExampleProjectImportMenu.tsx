import { importExampleProjectRequest } from "@ducks/workspace/requests";
import useErrorHandler from "../../../hooks/useErrorHandler";
import { useDispatch } from "react-redux";
import { Icon, shadcn } from "@eccenca/gui-elements";
import React from "react";
import { useTranslation } from "react-i18next";
import { routerOp } from "@ducks/router";
import { requestProjectMetadata } from "@ducks/shared/requests";
import { AppDispatch } from "store/configureStore";

const { DropdownMenuItem } = shadcn;

/** Menu item (native shadcn, used in the header Help menu) to load the "movies" example project. */
export const ExampleProjectImportMenu = () => {
    const { registerError } = useErrorHandler();
    const dispatch = useDispatch<AppDispatch>();
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
        <DropdownMenuItem data-test-id={"import-example-project"} disabled={loading} onClick={importExampleProject}>
            <Icon name="item-add-artefact" small />
            {t("common.action.loadExampleProject")}
        </DropdownMenuItem>
    ) : null;
};
