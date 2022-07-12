import React from "react";
import {
    Button,
    Icon,
    IconButton,
    Spacing,
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

/** Toolbar of the rule editor. Contains global editor actions like save, redo/undo etc. */
export const RuleEditorToolbar = () => {
    const modelContext = React.useContext(RuleEditorModelContext);
    const ruleEditorContext = React.useContext(RuleEditorContext);
    const ruleEditorUiContext = React.useContext(RuleEditorUiContext);
    const ruleEvaluationContext: RuleEditorEvaluationContextProps =
        React.useContext<RuleEditorEvaluationContextProps>(RuleEditorEvaluationContext);
    const [savingWorkflow, setSavingWorkflow] = React.useState(false);
    const [evaluationShown, setEvaluationShown] = React.useState(false);
    const [t] = useTranslation();
    const integratedView = !!ruleEditorContext.viewActions?.integratedView

    useHotKey({
        hotkey: "mod+z",
        handler: (e) => {
            e.preventDefault && e.preventDefault();
            modelContext.undo();
        },
        enabled: !ruleEditorUiContext.modalShown,
    });
    useHotKey({
        hotkey: "mod+shift+z",
        handler: (e) => {
            e.preventDefault && e.preventDefault();
            modelContext.redo();
        },
        enabled: !ruleEditorUiContext.modalShown,
    });

    // Warn of unsaved changes
    React.useEffect(() => {
        if (modelContext.unsavedChanges) {
            window.onbeforeunload = () => true;
        } else {
            window.onbeforeunload = null;
        }
        const parentWindow = window.parent as Window & {setLinkingEditorUnsavedChanges?: (hasUnsavedChanges: boolean) => any}
        if(integratedView && parentWindow !== window && typeof parentWindow.setLinkingEditorUnsavedChanges === "function") {
            try {
                parentWindow.setLinkingEditorUnsavedChanges(modelContext.unsavedChanges)
            } catch(ex) {
                console.warn("Cannot call setLinkingEditorUnsavedChanges() of parent window!", ex)
            }
        }
    }, [modelContext.unsavedChanges]);

    const saveLinkingRule = async (e) => {
        e.preventDefault();
        setSavingWorkflow(true);
        await modelContext.saveRule();
        setSavingWorkflow(false);
    };

    const startEvaluation = () => {
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

    return (
        <>
            {ruleEditorContext.editorTitle ? (
                <TitleMainsection>{ruleEditorContext.editorTitle}</TitleMainsection>
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
                        name="operation-auto-graph-layout"
                        text={t("RuleEditor.toolbar.autoLayout")}
                        onClick={() => modelContext.executeModelEditOperation.autoLayout(true)}
                    />
                    {ruleEvaluationContext.supportsEvaluation && (
                        <>
                            <Spacing vertical hasDivider />
                            <Switch
                                data-test-id={"rule-editor-evaluation-toggle"}
                                label={t("RuleEditor.toolbar.showEvaluation")}
                                checked={evaluationShown}
                                onClick={() => toggleEvaluation(!evaluationShown)}
                            />
                        </>
                    )}
                <Spacing vertical size={"small"}/>
                <Switch
                    data-test-id={"rule-editor-advanced-toggle"}
                    label={t("RuleEditor.toolbar.advancedParameterMode")}
                    checked={ruleEditorUiContext.advancedParameterModeEnabled}
                    onClick={() => ruleEditorUiContext.setAdvancedParameterMode(!ruleEditorUiContext.advancedParameterModeEnabled)}
                />
                </ToolbarSection>
                <ToolbarSection canGrow>
                    <Spacing vertical />
                </ToolbarSection>
                {ruleEditorContext.additionalToolBarComponents ? ruleEditorContext.additionalToolBarComponents() : null}
                {ruleEvaluationContext.evaluationResultsShown || ruleEvaluationContext.supportsEvaluation ? (
                    <ToolbarSection>
                        <EvaluationActivityControl
                            score={ruleEvaluationContext.evaluationScore}
                            loading={ruleEvaluationContext.evaluationRunning}
                            referenceLinksUrl={ruleEvaluationContext.referenceLinksUrl}
                            evaluationResultsShown={ruleEvaluationContext.evaluationResultsShown}
                            manualStartButton={{
                                "data-test-id": "rule-editor-start-evaluation-btn",
                                disabled: ruleEvaluationContext.evaluationRunning,
                                icon: "item-start",
                                tooltip: t("RuleEditor.toolbar.startEvaluation"),
                                action: startEvaluation,
                            }}
                        />
                        <Spacing vertical hasDivider />
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
                        tooltipProperties={{ hoverCloseDelay: 0 }}
                        onClick={saveLinkingRule}
                        disabled={modelContext.isReadOnly() || !modelContext.unsavedChanges}
                        href={modelContext.isReadOnly() || !modelContext.unsavedChanges ? "#" : undefined}
                        loading={savingWorkflow}
                    >
                        {modelContext.isReadOnly() ? (
                            <Icon name={"write-protected"} />
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
                    />
                </ToolbarSection>
            </Toolbar>
        </>
    );
};
