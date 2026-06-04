import React, { useMemo, useState } from "react";
import { NodeTools, NodeToolsMenuFunctions } from "@eccenca/gui-elements/src/extensions/react-flow/nodes/NodeTools";
import { Menu, MenuDivider, MenuItem } from "@eccenca/gui-elements";
import { RuleEditorUiContext } from "../../contexts/RuleEditorUiContext";
import { RuleEditorEvaluationContext } from "../../contexts/RuleEditorEvaluationContext";
import { RuleEditorModelContext } from "../../contexts/RuleEditorModelContext";
import { RuleEditorContext } from "../../contexts/RuleEditorContext";
import { ruleEditorModelUtilsFactory } from "../../model/RuleEditorModel.utils";
import { internalRuleBlockEvaluationActionState } from "./internalRuleBlockEvaluationAction.utils";
import { taskUrl } from "../../../../../store/ducks/router/operations";

interface NodeMenuProps {
    nodeId: string;
    t: (translationKey: string, defaultValue?: string) => string;
    handleDeleteNode: (nodeId: string) => void;
    handleCloneNode: (nodeId: string) => void;
    ruleOperatorDescription?: string;
    ruleOperatorDocumentation?: string;
    nodeType?: string;
    ruleOperatorLabel?: string;
}

/** The menu of a rule node. */
export const RuleNodeMenu = ({
    nodeId,
    t,
    handleDeleteNode,
    handleCloneNode,
    ruleOperatorDescription,
    ruleOperatorDocumentation,
    ruleOperatorLabel,
    nodeType,
}: NodeMenuProps) => {
    const [menuFns, setMenuFns] = useState<NodeToolsMenuFunctions | undefined>(undefined);
    const ruleEditorUiContext = React.useContext(RuleEditorUiContext);
    const ruleEvaluationContext = React.useContext(RuleEditorEvaluationContext);
    const modelContext = React.useContext(RuleEditorModelContext);
    const ruleEditorContext = React.useContext(RuleEditorContext);
    const [utils] = React.useState(ruleEditorModelUtilsFactory());

    const closeMenu = () => {
        menuFns?.closeMenu();
    };
    const menuFunctionsCallback = useMemo(() => (menuFunctions) => setMenuFns(menuFunctions), []);
    const operatorDoc = ruleOperatorDocumentation || ruleOperatorDescription || "";
    const currentRuleNode = modelContext.ruleOperatorNodes().find((node) => node.nodeId === nodeId);
    const extraMenuItems = currentRuleNode
        ? ruleEditorContext.extraRuleNodeMenuItems?.(currentRuleNode, closeMenu)
        : undefined;
    const internalRuleBlockEvaluationAction = internalRuleBlockEvaluationActionState(
        currentRuleNode,
        ruleEvaluationContext.evaluationResultsShown,
        ruleEvaluationContext.canEvaluateRuleBlock,
    );
    const openRuleBlockUrl =
        currentRuleNode?.pluginType === "RuleBlock" && ruleEditorContext.projectId
            ? taskUrl(ruleEditorContext.projectId, "RuleBlock", currentRuleNode.pluginId)
            : undefined;

    const nodeDimensions = utils.nodeById(modelContext.elements, nodeId)?.data.nodeDimensions;
    const resizeResetIsDisabled = !nodeDimensions?.width && !nodeDimensions?.height;

    return (
        <NodeTools menuButtonDataTestId={"node-menu-btn"} menuFunctionsCallback={menuFunctionsCallback}>
            <Menu>
                <MenuItem
                    data-test-id="rule-node-clone-btn"
                    key="clone"
                    icon={"item-clone"}
                    onClick={(e) => {
                        e.preventDefault();
                        handleCloneNode(nodeId);
                    }}
                    htmlTitle={"Hotkey: CTRL/CMD + d"}
                    text={t("common.action.clone")}
                />
                {ruleOperatorDescription || ruleOperatorDocumentation ? (
                    <MenuItem
                        data-test-id="rule-node-info"
                        key="info"
                        icon={"item-question"}
                        onClick={(e) => {
                            closeMenu();
                            ruleEditorUiContext.setCurrentRuleNodeInfo({
                                description: operatorDoc,
                                label: ruleOperatorLabel,
                            });
                            e.preventDefault();
                            e.stopPropagation();
                        }}
                        text={t("RuleEditor.node.menu.description.label")}
                        htmlTitle={ruleOperatorDescription}
                    />
                ) : null}
                {ruleEvaluationContext.canBeEvaluated(nodeType) ? (
                    <MenuItem
                        data-test-id="rule-node-evaluate-btn"
                        key="evaluate-subtree"
                        icon={"item-start"}
                        onClick={(e) => {
                            e.preventDefault();
                            ruleEvaluationContext.setEvaluationRootNode(nodeId);
                            const subtreeRuleOperatorNodes = modelContext.ruleOperatorNodes();
                            ruleEvaluationContext.startEvaluation(
                                subtreeRuleOperatorNodes,
                                ruleEditorContext.editedItem,
                                false,
                            );
                            ruleEvaluationContext.toggleEvaluationResults(true);
                        }}
                        text={t("RuleEditor.node.menu.subtree.label", "Evaluate subtree")}
                        htmlTitle={t(
                            "RuleEditor.node.menu.subtree.description",
                            "Evaluate linking tree partially until this operator node.",
                        )}
                    />
                ) : null}
                {openRuleBlockUrl ? (
                    <MenuItem
                        data-test-id="rule-node-open-rule-block-btn"
                        key="open-rule-block"
                        icon="item-viewdetails"
                        onClick={(e) => {
                            e.preventDefault();
                            closeMenu();
                            window.open(openRuleBlockUrl, "_blank", "noopener");
                        }}
                        text={t("RuleEditor.node.menu.openRuleBlock.label", "Open rule block")}
                        htmlTitle={t(
                            "RuleEditor.node.menu.openRuleBlock.description",
                            "Open the referenced rule block in a new tab.",
                        )}
                    />
                ) : null}
                {internalRuleBlockEvaluationAction.visible && currentRuleNode ? (
                    <MenuItem
                        data-test-id="rule-node-open-internal-rule-block-evaluation-btn"
                        key="open-internal-rule-block-evaluation"
                        icon="item-viewdetails"
                        disabled={!internalRuleBlockEvaluationAction.enabled}
                        onClick={(e) => {
                            e.preventDefault();
                            closeMenu();
                            ruleEvaluationContext.openInternalRuleBlockEvaluation?.(
                                nodeId,
                                currentRuleNode.pluginId,
                                currentRuleNode.label,
                            );
                        }}
                        text={t(
                            "RuleEditor.node.menu.openInternalRuleBlockEvaluation.label",
                            "Show internal evaluation",
                        )}
                        htmlTitle={t(
                            "RuleEditor.node.menu.openInternalRuleBlockEvaluation.description",
                            "Show the latest available internal evaluation for this reusable rule block usage.",
                        )}
                    />
                ) : null}
                <MenuItem
                    data-test-id="rule-node-reset-size-btn"
                    icon="item-reset"
                    disabled={resizeResetIsDisabled}
                    onClick={() => modelContext.executeModelEditOperation.changeSize(nodeId, undefined)}
                    text="Reset node size"
                ></MenuItem>
                <MenuDivider />
                {extraMenuItems?.length ? (
                    <>
                        {extraMenuItems}
                        <MenuDivider />
                    </>
                ) : null}
                <MenuItem
                    data-test-id="rule-node-delete-btn"
                    key="delete"
                    icon={"item-remove"}
                    onClick={(e) => {
                        e.preventDefault();
                        handleDeleteNode(nodeId);
                    }}
                    text={t("RuleEditor.node.menu.remove.label")}
                    htmlTitle={"Hotkey: <Backspace>"}
                    intent="danger"
                />
            </Menu>
        </NodeTools>
    );
};
