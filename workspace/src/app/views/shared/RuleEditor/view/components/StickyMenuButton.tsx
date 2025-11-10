import React from "react";
import { useTranslation } from "react-i18next";
import { IconButton, StickyNoteModal } from "@eccenca/gui-elements";
import { RuleEditorModelContext } from "../../contexts/RuleEditorModelContext";
import { StickyNoteMetadataType } from "@eccenca/gui-elements/src/cmem/react-flow/StickyNoteModal/StickyNoteModal";

interface StickyMenuButtonProps {
    stickyNodeId: string;
    color: string;
    stickyNote: string;
}

const StickyMenuButton: React.FC<StickyMenuButtonProps> = ({ stickyNodeId, color, stickyNote }) => {
    const [currentStickyContent, setCurrentStickyContent] = React.useState<StickyNoteMetadataType | undefined>(
        undefined,
    );
    const [showEditModal, setShowEditModal] = React.useState<boolean>(false);
    const [t] = useTranslation();
    const modelContext = React.useContext(RuleEditorModelContext);

    const translationsStickyNoteModal = {
        modalTitle: t("StickyNoteModal.title"),
        noteLabel: t("StickyNoteModal.labels.codeEditor"),
        colorLabel: t("StickyNoteModal.labels.color"),
        saveButton: t("common.action.save"),
        cancelButton: t("common.action.cancel"),
    };

    return (
        <>
            {showEditModal ? (
                <StickyNoteModal
                    simpleDialogProps={{
                        "data-test-id": "sticky-note-modal",
                    }}
                    metaData={currentStickyContent}
                    onClose={() => setShowEditModal(false)}
                    onSubmit={({ note, color }) =>
                        modelContext.executeModelEditOperation.changeStickyNodeProperties(stickyNodeId, color, note)
                    }
                    translate={(key) => translationsStickyNoteModal[key]}
                />
            ) : null}
            <IconButton
                data-test-id={"edit-sticky-note"}
                name="item-edit"
                text={t("RuleEditor.node.executionButtons.edit.tooltip")}
                onClick={() => {
                    setCurrentStickyContent({ note: stickyNote, color });
                    setShowEditModal(true);
                }}
                minimal={false}
            />
            <IconButton
                data-test-id={"remove-sticky-note"}
                name="item-remove"
                text={t("RuleEditor.node.menu.remove.label")}
                onClick={() => modelContext.executeModelEditOperation.deleteNode(stickyNodeId)}
                minimal={false}
            />
        </>
    );
};

export default StickyMenuButton;
