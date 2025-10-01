import {
    Button,
    Checkbox,
    FieldItem,
    Notification,
    SimpleDialog,
    Spacing,
    TextField,
    Tooltip,
} from "@eccenca/gui-elements";
import React, { useState } from "react";
import { referenceLinksChangeRequest } from "../LinkingEvaluationViewUtils";
import { useTranslation } from "react-i18next";
import { diErrorMessage } from "@ducks/error/typings";

interface Props {
    projectId: string;
    linkingTaskId: string;
    onClose: (needsRefresh: boolean) => any;
}

/** Modal to import reference links from a file (export).*/
export const ImportReferenceLinksModal = ({ projectId, linkingTaskId, onClose }: Props) => {
    const [t] = useTranslation();
    const [shouldGenerateNegativeLink, setShouldGenerateNegativeLink] = React.useState<boolean>(false);
    const [importedReferenceLinkFile, setImportedReferenceLinkFile] = React.useState<FormData>(new FormData());
    const [newLinkImportLoading, setNewLinkImportLoading] = React.useState<boolean>(false);
    const [errorMessage, setErrorMessage] = useState<string | null | undefined>(undefined);

    const cancel = () => onClose(false);
    const fileSelected = importedReferenceLinkFile.has("file");

    const handleImportReferenceLinks = React.useCallback(async () => {
        try {
            setNewLinkImportLoading(true);
            await referenceLinksChangeRequest(
                projectId,
                linkingTaskId,
                { generateNegative: shouldGenerateNegativeLink },
                "PUT",
                importedReferenceLinkFile
            );
            onClose(true);
        } catch (err) {
            const errorMessage = diErrorMessage(err) ?? "Reference links could not be imported.";
            setErrorMessage(errorMessage);
        } finally {
            setNewLinkImportLoading(false);
        }
    }, [shouldGenerateNegativeLink, importedReferenceLinkFile]);

    const handleImportReferenceLinkFile = (event: React.ChangeEvent<HTMLInputElement>) => {
        const file = event.target.files?.[0];
        if (file) {
            const newFormData = new FormData();
            newFormData.append("file", file);
            setImportedReferenceLinkFile(newFormData);
        }
    };

    return (
        <SimpleDialog
            isOpen={true}
            size="small"
            title={t("ReferenceLinks.importModal.title")}
            notifications={errorMessage ? <Notification message={errorMessage} intent="warning" /> : null}
            onClose={cancel}
            actions={[
                <Button
                    key="submit"
                    intent="primary"
                    onClick={handleImportReferenceLinks}
                    loading={newLinkImportLoading}
                    disabled={!fileSelected}
                >
                    {t("common.action.import")}
                </Button>,
                <Button key="cancel" onClick={cancel}>
                    {t("common.action.cancel")}
                </Button>,
            ]}
        >
            <>
                <FieldItem
                    labelProps={{
                        text: "File",
                    }}
                >
                    <TextField
                        type="file"
                        placeholder={t("common.action.chooseFile")}
                        onChange={handleImportReferenceLinkFile}
                    />
                </FieldItem>
                <Spacing size="small" />
                <Tooltip content={t("ReferenceLinks.importModal.generateDeclinedLinksTooltip")}>
                    <Checkbox
                        checked={shouldGenerateNegativeLink}
                        label={t("ReferenceLinks.importModal.generateDeclinedLinks")}
                        onChange={(e) => setShouldGenerateNegativeLink(e.currentTarget.checked)}
                    />
                </Tooltip>
            </>
        </SimpleDialog>
    );
};
