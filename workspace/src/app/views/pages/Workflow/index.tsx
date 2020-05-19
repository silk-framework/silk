import React, { useEffect } from "react";

import { useSelector } from "react-redux";
import { AppToaster } from "../../../services/toaster";
import { Intent } from "@wrappers/blueprint/constants";
import Metadata from "../../shared/Metadata";
import { datasetSel } from "@ducks/dataset";

import { Section, WorkspaceContent, WorkspaceMain, WorkspaceSide } from "@wrappers/index";
import { RelatedItems } from "../../shared/RelatedItems/RelatedItems";

export default function () {
    const error = useSelector(datasetSel.errorSelector);

    useEffect(() => {
        if (error.detail) {
            AppToaster.show({
                message: error.detail,
                intent: Intent.DANGER,
                timeout: 0,
            });
        }
    }, [error.detail]);

    return (
        <WorkspaceContent className="eccapp-di__workflow">
            <WorkspaceMain>
                <Section>
                    <Metadata />
                </Section>
            </WorkspaceMain>
            <WorkspaceSide>
                <Section>
                    <RelatedItems />
                </Section>
            </WorkspaceSide>
        </WorkspaceContent>
    );
}
