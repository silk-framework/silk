import { LinkTypeMapping, ReferenceLinkType } from "../typings";
import React, { useState } from "react";
import {
    Button,
    FieldItem,
    MenuItem,
    Notification,
    Select,
    SimpleDialog,
    Spacing,
    TextField,
} from "@eccenca/gui-elements";
import { updateReferenceLink } from "../LinkingEvaluationViewUtils";
import { useTranslation } from "react-i18next";
import { diErrorMessage } from "@ducks/error/typings";

interface Props {
    projectId: string;
    linkingTaskId: string;
    onClose: (needsRefresh: boolean) => any;
}

/** Modal to add a new reference link manually. */
export const AddReferenceLinkModal = ({ projectId, linkingTaskId, onClose }: Props) => {
    const [t] = useTranslation();
    const [errorMessage, setErrorMessage] = useState<string | null | undefined>(undefined);
    const [newSourceReferenceLink, setNewSourceReferenceLink] = React.useState<string>("");
    const [newLinkCreationLoading, setNewLinkCreationLoading] = React.useState<boolean>(false);
    const [newTargetReferenceLink, setNewTargetReferenceLink] = React.useState<string>("");
    const [newLinkType, setNewLinkType] = React.useState<ReferenceLinkType>("positive");

    const cancel = () => onClose(false);

    const handleAddNewReferenceLinks = React.useCallback(async () => {
        try {
            setNewLinkCreationLoading(true);
            await updateReferenceLink(
                projectId,
                linkingTaskId,
                newSourceReferenceLink,
                newTargetReferenceLink,
                newLinkType
            );
            onClose(true);
        } catch (err) {
            const errorMessage = diErrorMessage(err) ?? "Reference link could not be added.";
            setErrorMessage(errorMessage);
        } finally {
            setNewLinkCreationLoading(false);
        }
    }, [newSourceReferenceLink, newTargetReferenceLink, newLinkType]);

    return (
        <SimpleDialog
            isOpen={true}
            size="small"
            title={t("ReferenceLinks.addLinkModal.title")}
            onClose={cancel}
            data-test-id="add-reference-links-modal"
            notifications={errorMessage ? <Notification message={errorMessage} intent="warning" /> : null}
            actions={[
                <Button
                    key="submit"
                    intent="primary"
                    onClick={handleAddNewReferenceLinks}
                    loading={newLinkCreationLoading}
                    data-test-id="reference-links-add-submit"
                    disabled={
                        !newSourceReferenceLink ||
                        !newTargetReferenceLink ||
                        newSourceReferenceLink.trim() === "" ||
                        newTargetReferenceLink.trim() === ""
                    }
                >
                    {t("common.action.add")}
                </Button>,
                <Button key="cancel" onClick={cancel}>
                    {t("common.action.cancel")}
                </Button>,
            ]}
        >
            <FieldItem
                labelProps={{
                    text: t("ReferenceLinks.source"),
                }}
            >
                <TextField
                    data-test-id="new-source-reference-link"
                    value={newSourceReferenceLink}
                    placeholder={t("ReferenceLinks.addLinkModal.sourceUri")}
                    onChange={(e) => setNewSourceReferenceLink(e.target.value)}
                />
            </FieldItem>
            <Spacing size="small" />
            <FieldItem
                labelProps={{
                    text: t("ReferenceLinks.target"),
                }}
            >
                <TextField
                    data-test-id="new-target-reference-link"
                    value={newTargetReferenceLink}
                    placeholder={t("ReferenceLinks.addLinkModal.targetUri")}
                    onChange={(e) => setNewTargetReferenceLink(e.target.value)}
                />
            </FieldItem>
            <Spacing size="small" />
            <FieldItem
                labelProps={{
                    text: t("ReferenceLinks.type"),
                }}
            >
                <Select
                    items={["positive", "negative"].map((type) => ({ label: type }))}
                    onItemSelect={() => {}}
                    itemRenderer={(item, props) => {
                        return (
                            <MenuItem
                                data-test-id={`add-reference-${item.label}`}
                                text={t(`ReferenceLinks.${LinkTypeMapping[item.label]}`, LinkTypeMapping[item.label])}
                                onClick={() => setNewLinkType(item.label as ReferenceLinkType)}
                            />
                        );
                    }}
                    filterable={false}
                >
                    <Button
                        alignText="left"
                        text={t(`ReferenceLinks.${LinkTypeMapping[newLinkType]}`, LinkTypeMapping[newLinkType])}
                        fill
                        outlined
                        rightIcon="toggler-showmore"
                        data-test-id="reference-links-types-select"
                    />
                </Select>
            </FieldItem>
        </SimpleDialog>
    );
};
