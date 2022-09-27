import React, { useMemo, useState } from "react";
import { NodeTools, NodeToolsMenuFunctions } from "@eccenca/gui-elements/src/extensions/react-flow/nodes/NodeTools";
import { Menu, MenuItem } from "@eccenca/gui-elements";
import { RuleEditorUiContext } from "../../contexts/RuleEditorUiContext";

interface NodeMenuProps {
    nodeId: string;
    t: (translationKey: string, defaultValue?: string) => string;
    handleDeleteNode: (nodeId: string) => void;
    ruleOperatorDescription?: string;
}

/** The menu of a rule node. */
export const RuleNodeMenu = ({ nodeId, t, handleDeleteNode, ruleOperatorDescription }: NodeMenuProps) => {
    const [menuFns, setMenuFns] = useState<NodeToolsMenuFunctions | undefined>(undefined);
    const ruleEditorUiContext = React.useContext(RuleEditorUiContext);
    const closeMenu = () => {
        menuFns?.closeMenu();
    };
    const menuFunctionsCallback = useMemo(() => (menuFunctions) => setMenuFns(menuFunctions), []);

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
                    intent="danger"
                />
                {ruleOperatorDescription ? (
                    <MenuItem
                        data-test-id="rule-node-info"
                        key="info"
                        icon={"item-info"}
                        onClick={(e) => {
                            closeMenu();
                            ruleEditorUiContext.setCurrentRuleNodeDescription(ruleOperatorDescription);
                            e.preventDefault();
                            e.stopPropagation();
                        }}
                        text={t("RuleEditor.node.menu.description.label")}
                        htmlTitle={ruleOperatorDescription}
                    />
                ) : null}
            </Menu>
        </NodeTools>
    );
};
