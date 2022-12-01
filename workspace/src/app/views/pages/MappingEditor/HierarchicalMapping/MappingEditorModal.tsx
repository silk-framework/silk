import { Button, HtmlContentBlock, IconButton, SimpleDialog } from "@eccenca/gui-elements";
import React from "react";
import { useTranslation } from "react-i18next";

import { TransformRuleEditor } from "../../../../views/taskViews/transform/TransformRuleEditor";

export interface MappingEditorProps {
    /** Project ID the task is in. */
    projectId: string;
    /** The task the rules are being edited of. */
    transformTaskId: string;
    /** The transform rule that should be edited. This needs to be a value mapping rule. */
    ruleId: string;
    // control whether the modal is open or not
    isOpen: boolean;
    /**
     * utility to close the sticky note modal when cancelled as well as closed also
     */
    onClose: () => void;
}

const MappingEditorModal: React.FC<MappingEditorProps> = ({ ruleId, onClose, projectId, transformTaskId, isOpen }) => {
    /** keeps track of whether there are unsaved changes or not */
    const [unsavedChanges, setUnsavedChanges] = React.useState<boolean>(false);
    const [showWarningModal, setShowWarningModal] = React.useState<boolean>(false);
    const [t] = useTranslation();

    /**
     * handler to close editor if there no unsaved changes
     */
    const closeEditorModal = React.useCallback(() => {
        if (unsavedChanges) {
            setShowWarningModal(true);
        } else {
            onClose();
        }
    }, [unsavedChanges]);

    /** Warning prompt that shows up when the user decides to close the modal with unsaved changes */
    const WarningModal = React.memo(() => (
        <SimpleDialog
            intent="warning"
            data-test-id="mapping-editor-warning-modal"
            isOpen={showWarningModal}
            title="Unsaved changes"
            size="small"
            onClose={() => setShowWarningModal(false)}
            actions={[
                <Button
                    onClick={() => {
                        setShowWarningModal(false);
                        onClose();
                    }}
                >
                    {t("taskViews.transformRulesEditor.warning.modal.close-btn")}
                </Button>,
                <Button onClick={() => setShowWarningModal(false)}>
                    {t("taskViews.transformRulesEditor.warning.modal.back-btn")}
                </Button>,
            ]}
        >
            <HtmlContentBlock>
                <p>{t("taskViews.transformRulesEditor.warning.modal.body")}</p>
            </HtmlContentBlock>
        </SimpleDialog>
    ));

    return (
        <SimpleDialog
            data-test-id="transform-mapping-editor-modal"
            isOpen={isOpen}
            title="Value formula editor"
            size="fullscreen"
            canEscapeKeyClose={unsavedChanges}
            onClose={onClose}
            headerOptions={
                <IconButton
                    name="navigation-close"
                    text={t("common.action.close")}
                    onClick={closeEditorModal}
                    data-test-id="transform-mapping-editor-close-btn"
                />
            }
        >
            <>
                <WarningModal />
                <div style={{ position: "relative", height: "100%" }}>
                    <TransformRuleEditor
                        projectId={projectId}
                        ruleId={ruleId}
                        instanceId={"transform-rule-editor-modal-instance"}
                        transformTaskId={transformTaskId}
                        viewActions={{
                            savedChanges: (status) => setUnsavedChanges(status),
                            integratedView: true,
                        }}
                    />
                </div>
            </>
        </SimpleDialog>
    );
};

export default MappingEditorModal;
