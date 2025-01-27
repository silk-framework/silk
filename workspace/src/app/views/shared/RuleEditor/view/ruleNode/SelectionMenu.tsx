import React from "react";
import { EdgeTools } from "@eccenca/gui-elements/src/extensions/react-flow";
import { XYPosition } from "react-flow-renderer/dist/types";
import { Button, Spacing, Menu, MenuItem, MenuDivider } from "@eccenca/gui-elements";
import { useTranslation } from "react-i18next";

interface SelectionMenuProps {
    /** Position of the menu. */
    position: XYPosition;
    /** Called when menu should be closed. */
    onClose: () => any;
    /** Callback to remove edge. */
    removeSelection: () => any;
    /** Clone selection. */
    cloneSelection: () => any;
    /** Clone selection. */
    copySelection: () => any;
}

/**
 * 
 *            <MenuDivider />
                    <MenuItem
                        data-test-id="workflow-node-delete-btn"
                        key="delete"
                        icon={"item-remove"}
                        onClick={() => handleDeleteNode(nodeId)}
                        text={t("WorkflowEditor.node.menu.remove.label")}
                        intent="danger"
                    />
 * @returns 
 */
/** Rule edge menu. */
export const SelectionMenu = ({
    position,
    onClose,
    removeSelection,
    cloneSelection,
    copySelection,
}: SelectionMenuProps) => {
    const [t] = useTranslation();
    return (
        // FIXME: CMEM-3742: Use a generic "tools" component or rename EdgeTools
        <EdgeTools posOffset={{ left: position.x, top: position.y }} onClose={onClose}>
            <Menu>
                <MenuItem
                    text={t("RuleEditor.selection.menu.copy.label")}
                    icon="item-copy"
                    data-test-id={"selection-menu-copy-btn"}
                    htmlTitle={t("RuleEditor.selection.menu.copy.tooltip")}
                    onClick={() => {
                        onClose();
                        copySelection();
                    }}
                />
                <MenuItem
                    text={t("RuleEditor.selection.menu.clone.label")}
                    icon="item-clone"
                    data-test-id={"selection-menu-clone-btn"}
                    htmlTitle={t("RuleEditor.selection.menu.clone.tooltip")}
                    onClick={() => {
                        onClose();
                        cloneSelection();
                    }}
                />
                <MenuDivider />
                <MenuItem
                    icon="item-remove"
                    data-test-id={"selection-menu-remove-btn"}
                    htmlTitle={t("RuleEditor.selection.menu.delete.tooltip")}
                    onClick={() => {
                        onClose();
                        removeSelection();
                    }}
                    intent="danger"
                    text={t("RuleEditor.selection.menu.delete.label")}
                />
            </Menu>
        </EdgeTools>
    );
};
