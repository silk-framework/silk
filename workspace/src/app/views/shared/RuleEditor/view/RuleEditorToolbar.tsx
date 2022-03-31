import React from "react";
import { Button, ContextMenu, Icon, IconButton, Spacing, Toolbar, ToolbarSection, Notification } from "gui-elements";
import { RuleEditorModelContext } from "../contexts/RuleEditorModelContext";
import { useTranslation } from "react-i18next";
import { NotificationsMenu } from "../../ApplicationNotifications/NotificationsMenu";
import { RuleEditorContext } from "../contexts/RuleEditorContext";
import useHotKey from "../../HotKeyHandler/HotKeyHandler";

/** Toolbar of the rule editor. Contains global editor actions like save, redo/undo etc. */
export const RuleEditorToolbar = () => {
    const modelContext = React.useContext(RuleEditorModelContext);
    const ruleEditorContext = React.useContext(RuleEditorContext);
    const [savingWorkflow, setSavingWorkflow] = React.useState(false);
    const [t] = useTranslation();
    useHotKey({
        hotkey: "mod+z",
        handler: (e) => {
            e.preventDefault && e.preventDefault();
            modelContext.undo();
        },
    });
    useHotKey({
        hotkey: "mod+shift+z",
        handler: (e) => {
            e.preventDefault && e.preventDefault();
            modelContext.redo();
        },
    });

    const saveLinkingRule = async (e) => {
        e.preventDefault();
        setSavingWorkflow(true);
        await modelContext.saveRule();
        setSavingWorkflow(false);
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
                <Spacing vertical />
            </ToolbarSection>
            <ToolbarSection canGrow>
                <Spacing vertical />
            </ToolbarSection>
            {ruleEditorContext.additionalToolBarComponents ? ruleEditorContext.additionalToolBarComponents() : null}
            <ToolbarSection>
                {ruleEditorContext.viewActions?.integratedView && <NotificationsMenu />}
                {ruleEditorContext.lastSaveResult?.success === false ? (
                    <>
                        <Spacing vertical size="tiny" />
                        <ContextMenu
                            data-test-id={"ruleEditorToolbar-saveError-Btn"}
                            togglerElement="application-warning"
                            togglerText={ruleEditorContext.lastSaveResult!!.errorMessage}
                            fullWidth
                            // Errors are always shown initially
                            defaultIsOpen={true}
                        >
                            <Notification danger={true} key={"errorMessage"} iconName="state-warning">
                                <p>{ruleEditorContext.lastSaveResult!!.errorMessage}</p>
                            </Notification>
                            {(ruleEditorContext.lastSaveResult.nodeErrors ?? [])
                                .filter((nodeError) => nodeError.message)
                                .map((nodeError) => (
                                    <div key={nodeError.nodeId}>
                                        <Spacing size={"tiny"} />
                                        <Notification
                                            warning={true}
                                            iconName="state-warning"
                                            actions={
                                                <IconButton
                                                    data-test-id={"RuleEditorToolbar-nodeError-btn"}
                                                    name="item-viewdetails"
                                                    text={t("RuleEditor.toolbar.saveError.nodeError.tooltip")}
                                                    onClick={() => {
                                                        modelContext.centerNode(nodeError.nodeId);
                                                    }}
                                                />
                                            }
                                        >
                                            <p>{nodeError.message}</p>
                                        </Notification>
                                    </div>
                                ))}
                        </ContextMenu>
                    </>
                ) : null}
                <Button
                    data-test-id="ruleEditor-save-button"
                    affirmative={!modelContext.isReadOnly()}
                    tooltip={
                        modelContext.isReadOnly() ? t("RuleEditor.toolbar.readOnly") : t("RuleEditor.toolbar.save")
                    }
                    tooltipProperties={{ hoverCloseDelay: 0 }}
                    onClick={saveLinkingRule}
                    disabled={modelContext.isReadOnly() || !modelContext.unsavedChanges}
                    href={(modelContext.isReadOnly() || !modelContext.unsavedChanges) ? "#" : undefined}
                    loading={savingWorkflow}
                >
                    {modelContext.isReadOnly() ? <Icon name={"write-protected"} /> : t("common.action.save", "Save")}
                </Button>
            </ToolbarSection>
        </Toolbar>
    );
};
