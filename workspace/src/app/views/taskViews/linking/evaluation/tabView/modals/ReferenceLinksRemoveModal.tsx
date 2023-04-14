import { Button, Checkbox, OverviewItem, SimpleDialog, Spacing, Spinner } from "@eccenca/gui-elements";
import React from "react";
import { LinkTypeMapping, referenceLinksMap, ReferenceLinkType } from "../typings";
import { referenceLinksChangeRequest } from "../LinkingEvaluationViewUtils";

interface Props {
    projectId: string;
    linkingTaskId: string;
    /** Called when closing the modal. Boolean parameter signals if the reference links have been changed. */
    onClose: (needsRefresh: boolean) => any;
}

/** Allows to remove all confirmed/declined reference links. */
export const ReferenceLinksRemoveModal = ({ projectId, linkingTaskId, onClose }: Props) => {
    const [deleteReferenceLinkLoading, setDeleteReferenceLinkLoading] = React.useState<boolean>(false);
    const [deleteReferenceLinkMap, setDeleteReferenceLinkMap] =
        React.useState<Map<ReferenceLinkType, boolean>>(referenceLinksMap);
    const handleDeleteLinkTypeChecked = React.useCallback((linkType: ReferenceLinkType, isChecked: boolean) => {
        setDeleteReferenceLinkMap((prev) => new Map([...prev, [linkType, isChecked]]));
    }, []);

    const cancel = () => onClose(false);
    const closeAndRefresh = () => onClose(true);

    const handleDeleteReferenceLinks = React.useCallback(async () => {
        try {
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
        } finally {
            setDeleteReferenceLinkLoading(false);
        }
    }, [deleteReferenceLinkMap]);

    return (
        <SimpleDialog
            size="small"
            title="Remove Reference links"
            hasBorder
            isOpen={true}
            onClose={cancel}
            notifications={
                <p>
                    Reference links would be deleted for every of the selection above, please make sure you have checked
                    correctly
                </p>
            }
            actions={[
                <Button key="delete" hasStateDanger onClick={handleDeleteReferenceLinks}>
                    {deleteReferenceLinkLoading ? <Spinner size="tiny" /> : "Delete"}
                </Button>,
                <Button key="cancel" elevated onClick={cancel}>
                    Close
                </Button>,
            ]}
        >
            <OverviewItem>
                {Array.from(deleteReferenceLinkMap).map(([linkType, isChecked]) => (
                    <React.Fragment key={linkType}>
                        <Checkbox
                            value={linkType}
                            checked={isChecked}
                            label={LinkTypeMapping[linkType]}
                            key={linkType}
                            onChange={(e) => handleDeleteLinkTypeChecked(linkType, e.currentTarget.checked)}
                        />
                        <Spacing vertical size="tiny" />
                    </React.Fragment>
                ))}
            </OverviewItem>
        </SimpleDialog>
    );
};
