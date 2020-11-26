import React, { useEffect } from "react";

import { useSelector } from "react-redux";
import { AppToaster } from "../../../services/toaster";
import { Intent } from "@gui-elements/blueprint/constants";
import Metadata from "../../shared/Metadata";
import { datasetSel } from "@ducks/dataset";

import { Section, WorkspaceContent, WorkspaceMain, WorkspaceSide, Spacing } from "@gui-elements/index";
import { RelatedItems } from "../../shared/RelatedItems/RelatedItems";
import { LegacyWindow } from "../../shared/LegacyWindow/LegacyWindow";

export default function () {
    const error = useSelector(datasetSel.errorSelector);

    useEffect(() => {
        if (error?.detail) {
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
                    <Spacing />
                    <LegacyWindow />
                </Section>
            </WorkspaceMain>
            <WorkspaceSide>
                <Section>
                    <RelatedItems messageEventReloadTrigger={(messageId) => messageId === "workflowSaved"} />
                </Section>
            </WorkspaceSide>
        </WorkspaceContent>
    );
}
