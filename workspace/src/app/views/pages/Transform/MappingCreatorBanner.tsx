import { Button, Notification, Spacing } from "@eccenca/gui-elements";
import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import { useHistory, useLocation } from "react-router";
import { useSelector } from "react-redux";
import { commonSel } from "@ducks/common";
import { SERVE_PATH } from "../../../constants/path";
import { SUPPORTED_PLUGINS } from "../../plugins/PluginRegistry";

/** Local storage key that remembers that the user has dismissed the mapping creator announcement banner. */
const BANNER_DISMISSED_STORAGE_KEY = "di:mappingCreatorV2:bannerDismissed";

// Announcement banner shown on the transform task page that points users to the new mapping creator
// tab further down on the same page. Only shown while the backend `mappingCreatorEnabled` feature
// flag is on, until dismissed, and while the mapping creator tab is not already the active one.
export default function MappingCreatorBanner({ projectId, taskId }: { projectId: string; taskId: string }) {
    const [t] = useTranslation();
    const history = useHistory();
    const location = useLocation();
    const initialSettings = useSelector(commonSel.initialSettingsSelector);
    const [dismissed, setDismissed] = useState<boolean>(
        () => localStorage.getItem(BANNER_DISMISSED_STORAGE_KEY) === "true",
    );

    const mappingCreatorTabActive =
        location.pathname.split("/").slice(-1)[0] === SUPPORTED_PLUGINS.DI_MAPPING_CREATOR_V2;

    if (!initialSettings.mappingCreatorEnabled || dismissed || mappingCreatorTabActive) return null;

    const dismissBanner = () => {
        localStorage.setItem(BANNER_DISMISSED_STORAGE_KEY, "true");
        setDismissed(true);
    };

    const openMappingCreator = () => {
        // Tab bookmark URL of the task details page: keeps the workbench chrome and just
        // activates the mapping creator tab (ProjectTaskTabView syncs its tab with the URL).
        history.push(
            `${SERVE_PATH}/projects/${projectId}/transform/${taskId}/${SUPPORTED_PLUGINS.DI_MAPPING_CREATOR_V2}`,
        );
    };

    return (
        <>
            <Notification
                intent="info"
                actions={[
                    <Button
                        key="tryIt"
                        onClick={openMappingCreator}
                        text={t("pages.transform.mappingCreatorBanner.tryIt", "Open the new mapping creator")}
                        data-test-id="mapping-creator-banner-tryit-btn"
                    />,
                    <Spacing key="spacing" size="tiny" vertical />,
                    <Button
                        key="dismiss"
                        variant="ghost"
                        onClick={dismissBanner}
                        text={t("pages.transform.mappingCreatorBanner.dismiss", "Dismiss")}
                        data-test-id="mapping-creator-banner-dismiss-btn"
                    />,
                ]}
            >
                {t(
                    "pages.transform.mappingCreatorBanner.message",
                    "There is a new way to transform datasets into Knowledge Graphs — try the new mapping creator.",
                )}
            </Notification>
            <Spacing />
        </>
    );
}
