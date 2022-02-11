import React from "react";
import { NodeTools } from "gui-elements/src/extensions/react-flow/nodes/NodeTools";
import { Menu, MenuItem } from "gui-elements";

interface NodeMenuProps {
    nodeId: string;
    t: (translationKey: string, defaultValue?: string) => string;
    handleDeleteNode: (nodeId: string) => void;
}

/** The menu of a rule node. */
export const RuleNodeMenu = ({ nodeId, t, handleDeleteNode }: NodeMenuProps) => {
    return (
        <NodeTools menuButtonDataTestId={"node-menu-btn"}>
            <Menu>
                <MenuItem
                    data-test-id="rule-node-delete-btn"
                    key="delete"
                    icon={"item-remove"}
                    onClick={() => handleDeleteNode(nodeId)}
                    text={t("RuleEditor.node.menu.remove.label", "Remove")}
                    intent="danger"
                />
            </Menu>
        </NodeTools>
    );
};
