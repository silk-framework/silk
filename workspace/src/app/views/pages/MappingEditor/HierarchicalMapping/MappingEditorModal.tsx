import React from "react";
import { Button, SimpleDialog, Spacing } from "@eccenca/gui-elements";
import { TransformRuleEditor } from "../../../../views/taskViews/transform/TransformRuleEditor";
import EventEmitter from "./utils/EventEmitter";
import { MESSAGES } from "./utils/constants";

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
            data-test-id="mapping-editor-warning-modal"
            isOpen={showWarningModal}
            title="Editor Warning"
            size="small"
            onClose={() => setShowWarningModal(false)}
            actions={[
                <Button
                    disruptive={true}
                    onClick={() => {
                        setShowWarningModal(false);
                        onClose();
                    }}
                >
                    Yes, close
                </Button>,
                <Button onClick={() => setShowWarningModal(false)}>No, go back</Button>,
            ]}
        >
            <p>There are still unsaved changes. Are you sure you want to close the editor?</p>
        </SimpleDialog>
    ));

    return (
        <SimpleDialog
            data-test-id="mapping-editor-modal"
            isOpen={isOpen}
            title="Mapping Editor"
            size="fullscreen"
            canEscapeKeyClose={unsavedChanges}
            onClose={onClose}
        >
            <>
                <WarningModal />
                <div style={{ position: "relative", height: "100%" }}>
                    <TransformRuleEditor
                        projectId={projectId}
                        ruleId={ruleId}
                        transformTaskId={transformTaskId}
                        viewActions={{
                            savedChanges: (status) => setUnsavedChanges(status),
                            onSave: () => EventEmitter.emit(MESSAGES.RELOAD),
                        }}
                        additionalToolBarComponents={() => (
                            <>
                                <Button onClick={closeEditorModal}>Close</Button>
                                <Spacing vertical />
                            </>
                        )}
                    />
                </div>
            </>
        </SimpleDialog>
    );
};

export default MappingEditorModal;
