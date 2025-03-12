import React, { useMemo, useState } from "react";
import { NodeTools, NodeToolsMenuFunctions } from "@eccenca/gui-elements/src/extensions/react-flow/nodes/NodeTools";
import { Menu, MenuDivider, MenuItem } from "@eccenca/gui-elements";
import { RuleEditorUiContext } from "../../contexts/RuleEditorUiContext";
import { RuleEditorEvaluationContext } from "../../contexts/RuleEditorEvaluationContext";
import { RuleEditorModelContext } from "../../contexts/RuleEditorModelContext";
import { RuleEditorContext } from "../../contexts/RuleEditorContext";
import { ruleEditorModelUtilsFactory } from "../../model/RuleEditorModel.utils";

interface NodeMenuProps {
    nodeId: string;
    t: (translationKey: string, defaultValue?: string) => string;
    handleDeleteNode: (nodeId: string) => void;
    handleCloneNode: (nodeId: string) => void;
    ruleOperatorDescription?: string;
    ruleOperatorDocumentation?: string;
    nodeType?: string;
}

/** The menu of a rule node. */
export const RuleNodeMenu = ({
    nodeId,
    t,
    handleDeleteNode,
    handleCloneNode,
    ruleOperatorDescription,
    ruleOperatorDocumentation,
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
    const operatorDoc = `${ruleOperatorDescription ?? ""} ${
        ruleOperatorDocumentation ? `\n\n${ruleOperatorDocumentation}` : ""
    }`;

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
                    data-test-id="rule-node-reset-size-btn"
                    icon="item-reset"
                    disabled={resizeResetIsDisabled}
                    onClick={() => modelContext.executeModelEditOperation.changeSize(nodeId, undefined)}
                    text="Reset node size"
                ></MenuItem>
                <MenuDivider />
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
