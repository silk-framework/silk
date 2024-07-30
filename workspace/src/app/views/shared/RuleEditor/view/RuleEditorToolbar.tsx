import React from "react";
import {
    Button,
    Icon,
    IconButton,
    Markdown,
    Spacing,
    StickyNoteModal,
    Switch,
    TitleMainsection,
    Toolbar,
    ToolbarSection,
} from "@eccenca/gui-elements";
import { RuleEditorModelContext } from "../contexts/RuleEditorModelContext";
import { useTranslation } from "react-i18next";
import { RuleEditorContext } from "../contexts/RuleEditorContext";
import { RuleEditorNotifications } from "./RuleEditorNotifications";
import useHotKey from "../../HotKeyHandler/HotKeyHandler";
import { RuleEditorUiContext } from "../contexts/RuleEditorUiContext";
import { RuleEditorEvaluationContext, RuleEditorEvaluationContextProps } from "../contexts/RuleEditorEvaluationContext";
import { EvaluationActivityControl } from "./evaluation/EvaluationActivityControl";
import { Prompt } from "react-router";
import { RuleValidationError } from "../RuleEditor.typings";
import { DEFAULT_NODE_HEIGHT, DEFAULT_NODE_WIDTH } from "../model/RuleEditorModel.utils";
import { RuleEditorBaseModal } from "./components/RuleEditorBaseModal";
import { ReactFlowHotkeyContext } from "@eccenca/gui-elements/src/cmem/react-flow/extensions/ReactFlowHotkeyContext";
import { useEditorPromptHack } from "../../../../hooks/useEditorPromptHack";

/** Toolbar of the rule editor. Contains global editor actions like save, redo/undo etc. */
export const RuleEditorToolbar = () => {
    const modelContext = React.useContext(RuleEditorModelContext);
    const ruleEditorContext = React.useContext(RuleEditorContext);
    const ruleEditorUiContext = React.useContext(RuleEditorUiContext);
    const ruleEvaluationContext: RuleEditorEvaluationContextProps =
        React.useContext<RuleEditorEvaluationContextProps>(RuleEditorEvaluationContext);
    const [savingWorkflow, setSavingWorkflow] = React.useState(false);
    const [evaluationShown, setEvaluationShown] = React.useState(false);
    const [showCreateStickyModal, setShowCreateStickyModal] = React.useState<boolean>(false);
    const [t] = useTranslation();
    const integratedView = !!ruleEditorContext.viewActions?.integratedView;
    const { hotKeysDisabled } = React.useContext(ReactFlowHotkeyContext);

    useHotKey({
        hotkey: "mod+z",
        handler: (e) => {
            e.preventDefault && e.preventDefault();
            modelContext.undo();
        },
        enabled: !ruleEditorUiContext.modalShown && !hotKeysDisabled,
    });
    useHotKey({
        hotkey: "mod+shift+z",
        handler: (e) => {
            e.preventDefault && e.preventDefault();
            modelContext.redo();
        },
        enabled: !ruleEditorUiContext.modalShown && !hotKeysDisabled,
    });

    /** make sure workflow editor on overlay doesn't close when there are unsaved changes */
    useEditorPromptHack(modelContext.unsavedChanges);

    // Warn of unsaved changes
    React.useEffect(() => {
        if (modelContext.unsavedChanges) {
            window.onbeforeunload = () => true;
        } else {
            window.onbeforeunload = null;
        }
        const parentWindow = window.parent as Window & {
            setLinkingEditorUnsavedChanges?: (hasUnsavedChanges: boolean) => any;
        };
        if (
            integratedView &&
            parentWindow !== window &&
            typeof parentWindow.setLinkingEditorUnsavedChanges === "function"
        ) {
            try {
                parentWindow.setLinkingEditorUnsavedChanges(modelContext.unsavedChanges);
            } catch (ex) {
                console.warn("Cannot call setLinkingEditorUnsavedChanges() of parent window!", ex);
            }
        }
    }, [modelContext.unsavedChanges]);

    React.useEffect(() => {
        ruleEvaluationContext.fetchTriggerEvaluationFunction?.(startEvaluation);
    }, [ruleEvaluationContext.startEvaluation, ruleEvaluationContext.toggleEvaluationResults]);

    const saveLinkingRule = async (e) => {
        e.preventDefault();
        setSavingWorkflow(true);
        await modelContext.saveRule();
        setSavingWorkflow(false);
    };

    // Start evaluation for the whole rule tree
    const startEvaluation = () => {
        // Reset sub-tre evaluation if set
        ruleEvaluationContext.setEvaluationRootNode(undefined);
        ruleEvaluationContext.startEvaluation(modelContext.ruleOperatorNodes(), ruleEditorContext.editedItem, false);
        toggleEvaluation(true);
    };

    const toggleEvaluation = (show: boolean) => {
        ruleEvaluationContext.toggleEvaluationResults(show);
        setEvaluationShown(show);
    };

    // Show 'unsaved changes' prompt when navigating away via React routing
    const routingPrompt = (/*location: H.Location, action: H.Action*/): string | boolean => {
        // At the moment it will complain on any kind of routing change
        return modelContext.unsavedChanges ? (t("taskViews.ruleEditor.warnings.unsavedChanges") as string) : true;
    };

    const ruleValidationError: RuleValidationError | undefined = ruleEvaluationContext.ruleValidationError
        ? ruleEvaluationContext.ruleValidationError
        : ruleEditorContext.lastSaveResult?.errorMessage
        ? (ruleEditorContext.lastSaveResult as RuleValidationError)
        : undefined;
    const translationsStickyNoteModal = {
        modalTitle: t("StickyNoteModal.title"),
        noteLabel: t("StickyNoteModal.labels.codeEditor"),
        colorLabel: t("StickyNoteModal.labels.color"),
        saveButton: t("common.action.save"),
        cancelButton: t("common.action.cancel"),
    };

    const handleStickyNoteSubmit = ({ note, color }) => {
        if (ruleEditorUiContext.reactFlowInstance && ruleEditorUiContext.reactFlowWrapper?.current) {
            const reactFlowBounds = ruleEditorUiContext.reactFlowWrapper.current.getBoundingClientRect();
            const position = ruleEditorUiContext.reactFlowInstance.project({
                x: (reactFlowBounds.width - DEFAULT_NODE_WIDTH) / 2,
                y: (reactFlowBounds.height - DEFAULT_NODE_HEIGHT) / 2,
            });
            modelContext.executeModelEditOperation.addStickyNode(note, position, color);
        } else {
            console.warn("No react-flow objects loaded!");
        }
    };

    return (
        <>
            {ruleEditorContext.editorTitle ? (
                <TitleMainsection>{ruleEditorContext.editorTitle}</TitleMainsection>
            ) : null}

            {ruleEditorUiContext.currentRuleNodeDescription ? (
                <RuleEditorBaseModal
                    isOpen={true}
                    title={t("common.words.description")}
                    onClose={() => ruleEditorUiContext.setCurrentRuleNodeDescription(undefined)}
                    hasBorder={true}
                    size={"small"}
                    data-test-id={"ruleEditorNode-description-modal"}
                    actions={[
                        <Button
                            key="close"
                            onClick={() => ruleEditorUiContext.setCurrentRuleNodeDescription(undefined)}
                        >
                            {t("common.action.close")}
                        </Button>,
                    ]}
                >
                    <Markdown>{ruleEditorUiContext.currentRuleNodeDescription}</Markdown>
                </RuleEditorBaseModal>
            ) : null}
            {showCreateStickyModal ? (
                <StickyNoteModal
                    onClose={() => setShowCreateStickyModal(false)}
                    onSubmit={handleStickyNoteSubmit}
                    translate={(key) => translationsStickyNoteModal[key]}
                />
            ) : null}
            <Toolbar data-test-id={"workflow-editor-header"} noWrap>
                <Prompt when={modelContext.unsavedChanges} message={routingPrompt} />
                <ToolbarSection>
                    <IconButton
                        data-test-id={"rule-editor-undo-btn"}
                        disabled={modelContext.isReadOnly() || !modelContext.canUndo}
                        name="operation-undo"
                        text="Undo"
                        onClick={modelContext.undo}
                    />
                    <IconButton
                        data-test-id={"rule-editor-redo-btn"}
                        disabled={modelContext.isReadOnly() || !modelContext.canRedo}
                        name="operation-redo"
                        text="Redo"
                        onClick={modelContext.redo}
                    />
                    <Spacing vertical hasDivider />
                    <IconButton
                        data-test-id={"rule-editor-auto-layout-btn"}
                        disabled={modelContext.isReadOnly() || modelContext.elements.length === 0}
                        name="operation-autolayout"
                        text={t("RuleEditor.toolbar.autoLayout")}
                        onClick={() => modelContext.executeModelEditOperation.autoLayout(true)}
                    />
                    <Spacing vertical size={"small"} />
                    <IconButton
                        data-test-id="rule-editor-header-sticky-btn"
                        name="item-comment"
                        text={t("StickyNoteModal.tooltip")}
                        onClick={() => setShowCreateStickyModal(true)}
                    />
                    <Spacing vertical size={"small"} />
                    <Switch
                        data-test-id={"rule-editor-advanced-toggle"}
                        label={t("RuleEditor.toolbar.advancedParameterMode")}
                        checked={ruleEditorUiContext.advancedParameterModeEnabled}
                        onClick={() =>
                            ruleEditorUiContext.setAdvancedParameterMode(
                                !ruleEditorUiContext.advancedParameterModeEnabled
                            )
                        }
                    />
                </ToolbarSection>
                <ToolbarSection canGrow>
                    <Spacing vertical size={"small"} />
                </ToolbarSection>
                {ruleEditorContext.additionalToolBarComponents ? ruleEditorContext.additionalToolBarComponents() : null}
                {ruleEvaluationContext.evaluationResultsShown || ruleEvaluationContext.supportsEvaluation ? (
                    <ToolbarSection>
                        <EvaluationActivityControl
                            score={ruleEvaluationContext.evaluationScore}
                            loading={ruleEvaluationContext.evaluationRunning}
                            referenceLinksUrl={ruleEvaluationContext.referenceLinksUrl}
                            evaluationResultsShown={ruleEvaluationContext.evaluationResultsShown}
                            evaluationResultsShownToggleButton={{
                                "data-test-id": "rule-editor-start-evaluation-btn",
                                disabled: ruleEvaluationContext.evaluationRunning,
                                icon: evaluationShown ? "item-hidedetails" : "item-viewdetails",
                                tooltip: evaluationShown
                                    ? t("RuleEditor.toolbar.hideEvaluation")
                                    : t("RuleEditor.toolbar.showEvaluation"),
                                action: () => toggleEvaluation(!evaluationShown),
                            }}
                            manualStartButton={{
                                "data-test-id": "rule-editor-start-evaluation-btn",
                                disabled: ruleEvaluationContext.evaluationRunning,
                                icon: "item-start",
                                tooltip: t("RuleEditor.toolbar.startEvaluation"),
                                action: startEvaluation,
                            }}
                        />
                        <Spacing vertical size="small" />
                    </ToolbarSection>
                ) : null}
                <ToolbarSection>
                    <Button
                        key={"save-button"}
                        data-test-id="ruleEditor-save-button"
                        affirmative={!modelContext.isReadOnly()}
                        tooltip={
                            modelContext.isReadOnly() ? t("RuleEditor.toolbar.readOnly") : t("RuleEditor.toolbar.save")
                        }
                        tooltipProps={{ hoverCloseDelay: 0 }}
                        onClick={saveLinkingRule}
                        disabled={modelContext.isReadOnly() || !modelContext.unsavedChanges}
                        href={modelContext.isReadOnly() || !modelContext.unsavedChanges ? "#" : undefined}
                        loading={savingWorkflow}
                    >
                        {modelContext.isReadOnly() ? (
                            <Icon name={"state-protected"} />
                        ) : (
                            t("common.action.save", "Save")
                        )}
                    </Button>
                    <RuleEditorNotifications
                        key={"notifications"}
                        integratedView={integratedView}
                        queueEditorNotifications={
                            ruleValidationError ? [ruleValidationError.errorMessage] : ([] as string[])
                        }
                        queueNodeNotifications={(ruleValidationError?.nodeErrors ?? []).filter(
                            (nodeError) => nodeError.message
                        )}
                        nodeJumpToHandler={modelContext.centerNode}
                        evaluationNotifications={ruleEvaluationContext.notifications}
                    />
                </ToolbarSection>
            </Toolbar>
        </>
    );
};
