import { Button, Checkbox, Notification, OverviewItem, SimpleDialog, Spacing } from "@eccenca/gui-elements";
import React, { useState } from "react";
import { LinkTypeMapping, referenceLinksMap, ReferenceLinkType } from "../typings";
import { referenceLinksChangeRequest } from "../LinkingEvaluationViewUtils";
import { useTranslation } from "react-i18next";
import { diErrorMessage } from "@ducks/error/typings";

interface Props {
    projectId: string;
    linkingTaskId: string;
    /** Called when closing the modal. Boolean parameter signals if the reference links have been changed. */
    onClose: (needsRefresh: boolean) => any;
}

/** Allows to remove all confirmed/declined reference links. */
export const ReferenceLinksRemoveModal = ({ projectId, linkingTaskId, onClose }: Props) => {
    const [t] = useTranslation();
    const [deleteReferenceLinkLoading, setDeleteReferenceLinkLoading] = React.useState<boolean>(false);
    const [deleteReferenceLinkMap, setDeleteReferenceLinkMap] =
        React.useState<Map<ReferenceLinkType, boolean>>(referenceLinksMap);
    const [errorMessage, setErrorMessage] = useState<string | null | undefined>(undefined);
    const handleDeleteLinkTypeChecked = React.useCallback((linkType: ReferenceLinkType, isChecked: boolean) => {
        setDeleteReferenceLinkMap((prev) => new Map([...prev, [linkType, isChecked]]));
    }, []);

    const cancel = () => onClose(false);
    const closeAndRefresh = () => onClose(true);

    const anyCheckboxSelected = [...deleteReferenceLinkMap.values()].some((v) => v);

    const handleDeleteReferenceLinks = React.useCallback(async () => {
        try {
            setErrorMessage(undefined);
            setDeleteReferenceLinkLoading(true);
            await referenceLinksChangeRequest(
                projectId,
                linkingTaskId,
                {
                    positive: deleteReferenceLinkMap.get("positive")!,
                    negative: deleteReferenceLinkMap.get("negative")!,
                    unlabeled: deleteReferenceLinkMap.get("unlabeled")!,
                },
                "DELETE"
            );
            closeAndRefresh();
        } catch (err) {
            const errorMessage = diErrorMessage(err) ?? "Reference links could not be deleted.";
            setErrorMessage(errorMessage);
        } finally {
            setDeleteReferenceLinkLoading(false);
        }
    }, [deleteReferenceLinkMap]);

    return (
        <SimpleDialog
            size="small"
            title={t("ReferenceLinks.removeModal.title")}
            notifications={errorMessage ? <Notification message={errorMessage} intent="warning" /> : null}
            isOpen={true}
            onClose={cancel}
            data-test-id="remove-reference-links-modal"
            actions={[
                <Button
                    key="delete"
                    intent="danger"
                    onClick={handleDeleteReferenceLinks}
                    disabled={!anyCheckboxSelected}
                    loading={deleteReferenceLinkLoading}
                >
                    {t("common.action.delete")}
                </Button>,
                <Button key="cancel" onClick={cancel}>
                    {t("common.action.cancel")}
                </Button>,
            ]}
        >
            <Spacing size={"small"} />
            <OverviewItem>
                {Array.from(deleteReferenceLinkMap).map(([linkType, isChecked]) => (
                    <React.Fragment key={linkType}>
                        <Checkbox
                            value={linkType}
                            checked={isChecked}
                            label={t(`ReferenceLinks.${LinkTypeMapping[linkType]}`, LinkTypeMapping[linkType])}
                            key={linkType}
                            onChange={(e) => handleDeleteLinkTypeChecked(linkType, e.currentTarget.checked)}
                        />
                        <Spacing vertical size="tiny" />
                    </React.Fragment>
                ))}
            </OverviewItem>
            <Notification message={t("ReferenceLinks.removeModal.infoMessage")} />
        </SimpleDialog>
    );
};
