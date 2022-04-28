import React from "react";
import { NodeTools } from "@eccenca/gui-elements/src/extensions/react-flow/nodes/NodeTools";
import { Menu, MenuItem } from "@eccenca/gui-elements";

interface NodeMenuProps {
    nodeId: string;
    t: (translationKey: string, defaultValue?: string) => string;
    handleDeleteNode: (nodeId: string) => void;
    ruleOperatorDescription?: string;
}

/** The menu of a rule node. */
export const RuleNodeMenu = ({ nodeId, t, handleDeleteNode, ruleOperatorDescription }: NodeMenuProps) => {
    return (
        <NodeTools menuButtonDataTestId={"node-menu-btn"}>
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
                            e.preventDefault();
                            e.stopPropagation();
                        }}
                        text={t("RuleEditor.node.menu.description.label")}
                        internalProps={{
                            htmlTitle: ruleOperatorDescription,
                        }}
                    />
                ) : null}
            </Menu>
        </NodeTools>
    );
};
