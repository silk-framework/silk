import React from "react";
import { IconButton, SimpleDialog, shadcn } from "@eccenca/gui-elements";
import { TransformRuleEditor } from "../../../../views/taskViews/transform/TransformRuleEditor";
import { useTranslation } from "react-i18next";
import { IViewActions } from "../../../../views/plugins/PluginRegistry";
import { InitialRuleHighlighting, RuleParameterType } from "../../../taskViews/transform/transform.types";

const {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogOverlay,
    AlertDialogPortal,
    AlertDialogTitle,
} = shadcn;

export interface MappingEditorProps {
    /** Project ID the task is in. */
    projectId: string;
    /** The task the rules are being edited of. */
    transformTaskId: string;
    /** The container rule ID, i.e. of either the root or an object rule. */
    containerRuleId: string;
    /** The transform rule that should be edited. This needs to be a value mapping rule. */
    ruleDefinition: RuleParameterType;
    // control whether the modal is open or not
    isOpen: boolean;
    /**
     * utility to close the sticky note modal when cancelled as well as closed also
     */
    onClose: () => void;
    /** Generic actions and callbacks on views. */
    viewActions?: IViewActions;
    /** Initially highlights the given operator nodes and shows a message explaining why the nodes are highlighted.
     * When the notification is closed the highlighting of the nodes is removed again.  */
    initialHighlighting?: InitialRuleHighlighting;
}

const MappingEditorModal = ({
    ruleDefinition,
    onClose,
    projectId,
    transformTaskId,
    isOpen,
    containerRuleId,
    viewActions,
    initialHighlighting,
}: MappingEditorProps) => {
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

    const updateViewActionUnsavedChanges = (status: boolean) => {
        viewActions?.unsavedChanges && viewActions.unsavedChanges(status);
    };

    /** Warning prompt that shows up when the user decides to close the modal with unsaved changes.
     * The surrounding fullscreen editor modal is a legacy dialog stacked at
     * `--eccgui-zindex-modals` (8001), far above the shadcn default of `z-50` — so the
     * overlay/content are elevated explicitly to stay on top of it. */
    const warningModal = (
        <AlertDialog open={showWarningModal} onOpenChange={(open) => !open && setShowWarningModal(false)}>
            {/* AlertDialogContent brings its own overlay, but that one is fixed at z-50 and
                disappears behind the fullscreen modal; this portal renders the visible backdrop. */}
            <AlertDialogPortal>
                <AlertDialogOverlay className="z-[8002]" />
            </AlertDialogPortal>
            <AlertDialogContent className="z-[8003]" data-test-id="mapping-editor-warning-modal">
                <AlertDialogHeader>
                    <AlertDialogTitle>{t("taskViews.transformRulesEditor.warning.modal.title")}</AlertDialogTitle>
                    <AlertDialogDescription>
                        {t("taskViews.transformRulesEditor.warning.modal.body")}
                    </AlertDialogDescription>
                </AlertDialogHeader>
                <AlertDialogFooter>
                    <AlertDialogCancel>{t("common.action.cancel")}</AlertDialogCancel>
                    <AlertDialogAction
                        variant="destructive"
                        onClick={() => {
                            setShowWarningModal(false);
                            onClose();
                            updateViewActionUnsavedChanges(false);
                        }}
                    >
                        {t("taskViews.transformRulesEditor.warning.modal.discard")}
                    </AlertDialogAction>
                </AlertDialogFooter>
            </AlertDialogContent>
        </AlertDialog>
    );

    return (
        <SimpleDialog
            data-test-id="transform-mapping-editor-modal"
            isOpen={isOpen}
            title="Value formula editor"
            size="fullscreen"
            preventSimpleClosing={unsavedChanges}
            onClose={onClose}
            wrapperDivProps={{
                onMouseUp: () => {},
                onMouseDown: () => {}, // do not stop event propagation, otherwise notification overlays will prevent usage of input fields
            }}
            headerOptions={
                <IconButton
                    name="navigation-close"
                    text={t("common.action.close")}
                    onClick={closeEditorModal}
                    data-test-id="transform-mapping-editor-close-btn"
                />
            }
            preventReactFlowEvents={false}
        >
            <>
                {warningModal}
                <div style={{ position: "relative", height: "100%" }}>
                    <TransformRuleEditor
                        projectId={projectId}
                        containerRuleId={containerRuleId}
                        ruleDefinition={ruleDefinition}
                        instanceId={"transform-rule-editor-modal-instance"}
                        transformTaskId={transformTaskId}
                        viewActions={{
                            unsavedChanges: (status) => {
                                setUnsavedChanges(status); // trigger the internal prompt
                                updateViewActionUnsavedChanges(status); //notify the views controller
                            },
                            integratedView: true,
                        }}
                        initialHighlighting={initialHighlighting}
                    />
                </div>
            </>
        </SimpleDialog>
    );
};

export default MappingEditorModal;
