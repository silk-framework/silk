import React from "react";
import { Button, Icon, IconButton, Spacing, Switch, Toolbar, ToolbarSection } from "gui-elements";
import { RuleEditorModelContext } from "../contexts/RuleEditorModelContext";
import { useTranslation } from "react-i18next";
import { RuleEditorContext } from "../contexts/RuleEditorContext";
import { RuleEditorNotifications } from "./RuleEditorNotifications";
import useHotKey from "../../HotKeyHandler/HotKeyHandler";
import { RuleEditorUiContext } from "../contexts/RuleEditorUiContext";
import { RuleEditorEvaluationContext, RuleEditorEvaluationContextProps } from "../contexts/RuleEditorEvaluationContext";
import { EvaluationScore } from "./evaluation/EvaluationScore";

/** Toolbar of the rule editor. Contains global editor actions like save, redo/undo etc. */
export const RuleEditorToolbar = () => {
    const ruleEditor = React.useContext(RuleEditorContext);
    const modelContext = React.useContext(RuleEditorModelContext);
    const ruleEditorContext = React.useContext(RuleEditorContext);
    const ruleEditorUiContext = React.useContext(RuleEditorUiContext);
    const ruleEvaluationContext: RuleEditorEvaluationContextProps =
        React.useContext<RuleEditorEvaluationContextProps>(RuleEditorEvaluationContext);
    const [savingWorkflow, setSavingWorkflow] = React.useState(false);
    const [evaluationShown, setEvaluationShown] = React.useState(false);
    const [t] = useTranslation();

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

    const saveLinkingRule = async (e) => {
        e.preventDefault();
        setSavingWorkflow(true);
        await modelContext.saveRule();
        setSavingWorkflow(false);
    };

    const startEvaluation = () => {
        ruleEvaluationContext.startEvaluation(modelContext.ruleOperatorNodes(), ruleEditor.editedItem, false);
        toggleEvaluation(true);
    };

    const toggleEvaluation = (show: boolean) => {
        ruleEvaluationContext.toggleEvaluationResults(show);
        setEvaluationShown(show);
    };

    return (
        <Toolbar data-test-id={"workflow-editor-header"} noWrap>
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
                    onClick={modelContext.executeModelEditOperation.autoLayout}
                />
                <Spacing vertical hasDivider />
                {ruleEvaluationContext.supportsEvaluation && (
                    <IconButton
                        data-test-id={"rule-editor-start-evaluation-btn"}
                        disabled={ruleEvaluationContext.evaluationRunning}
                        name="item-start"
                        text={t("RuleEditor.toolbar.startEvaluation")}
                        onClick={startEvaluation}
                        loading={ruleEvaluationContext.evaluationRunning}
                    />
                )}
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
            </ToolbarSection>
            <ToolbarSection canGrow>
                <Spacing vertical />
            </ToolbarSection>
            {ruleEditorContext.additionalToolBarComponents ? ruleEditorContext.additionalToolBarComponents() : null}
            {ruleEvaluationContext.evaluationScore ? (
                <ToolbarSection>
                    <EvaluationScore
                        score={ruleEvaluationContext.evaluationScore}
                        loading={ruleEvaluationContext.evaluationRunning}
                    />
                    <Spacing vertical hasDivider />
                </ToolbarSection>
            ) : null}
            <ToolbarSection>
                <Button
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
                    {modelContext.isReadOnly() ? <Icon name={"write-protected"} /> : t("common.action.save", "Save")}
                </Button>
                <RuleEditorNotifications
                    integratedView={ruleEditorContext.viewActions?.integratedView}
                    queueEditorNotifications={
                        ruleEditorContext.lastSaveResult?.errorMessage
                            ? [ruleEditorContext.lastSaveResult?.errorMessage]
                            : ([] as string[])
                    }
                    queueNodeNotifications={(ruleEditorContext.lastSaveResult?.nodeErrors ?? []).filter(
                        (nodeError) => nodeError.message
                    )}
                    nodeJumpToHandler={modelContext.centerNode}
                />
            </ToolbarSection>
        </Toolbar>
    );
};
