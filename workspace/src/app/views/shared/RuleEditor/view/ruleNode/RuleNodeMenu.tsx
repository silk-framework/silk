import React, { useMemo, useState } from "react";
import { NodeTools, NodeToolsMenuFunctions } from "@eccenca/gui-elements/src/extensions/react-flow/nodes/NodeTools";
import { Menu, MenuItem } from "@eccenca/gui-elements";
import { RuleEditorUiContext } from "../../contexts/RuleEditorUiContext";
import { RuleEditorEvaluationContext } from "../../contexts/RuleEditorEvaluationContext";
import { RuleEditorModelContext } from "../../contexts/RuleEditorModelContext";
import { RuleEditorContext } from "../../contexts/RuleEditorContext";

interface NodeMenuProps {
    nodeId: string;
    t: (translationKey: string, defaultValue?: string) => string;
    handleDeleteNode: (nodeId: string) => void;
    handleCloneNode: (nodeId: string) => void;
    ruleOperatorDescription?: string;
    ruleOperatorDocumentation?: string;
    nodeType?: string;
    handleNodeSizeReset?: (nodeId: string) => void;
}

/** The menu of a rule node. */
export const RuleNodeMenu = ({
    nodeId,
    t,
    handleDeleteNode,
    handleCloneNode,
    handleNodeSizeReset,
    ruleOperatorDescription,
    ruleOperatorDocumentation,
    nodeType,
}: NodeMenuProps) => {
    const [menuFns, setMenuFns] = useState<NodeToolsMenuFunctions | undefined>(undefined);
    const ruleEditorUiContext = React.useContext(RuleEditorUiContext);
    const ruleEvaluationContext = React.useContext(RuleEditorEvaluationContext);
    const modelContext = React.useContext(RuleEditorModelContext);
    const ruleEditorContext = React.useContext(RuleEditorContext);

    const closeMenu = () => {
        menuFns?.closeMenu();
    };
    const menuFunctionsCallback = useMemo(() => (menuFunctions) => setMenuFns(menuFunctions), []);
    const operatorDoc = `${ruleOperatorDescription ?? ""} ${
        ruleOperatorDocumentation ? `\n\n${ruleOperatorDocumentation}` : ""
    }`;

    return (
        <NodeTools menuButtonDataTestId={"node-menu-btn"} menuFunctionsCallback={menuFunctionsCallback}>
            <Menu>
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
                        icon={"item-info"}
                        onClick={(e) => {
                            closeMenu();
                            ruleEditorUiContext.setCurrentRuleNodeDescription(operatorDoc);
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
                                false
                            );
                            ruleEvaluationContext.toggleEvaluationResults(true);
                        }}
                        text={t("RuleEditor.node.menu.subtree.label", "Evaluate subtree")}
                        htmlTitle={t(
                            "RuleEditor.node.menu.subtree.description",
                            "Evaluate linking tree partially until this operator node."
                        )}
                    />
                ) : null}
                <MenuItem
                    data-test-id="rule-node-evaluate-btn"
                    icon="item-reset"
                    disabled={!modelContext.resizedNodes.get(nodeId)?.changed}
                    onClick={() => modelContext.resetNodeSize(nodeId)}
                    text="Reset node size"
                ></MenuItem>
            </Menu>
        </NodeTools>
    );
};
