import React from "react";
import { XYPosition } from "react-flow-renderer/dist/types";
import { ContextMenu, MenuItem, MenuDivider } from "@eccenca/gui-elements";
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

export const SelectionMenu = ({
    position,
    onClose,
    removeSelection,
    cloneSelection,
    copySelection,
}: SelectionMenuProps) => {
    const [t] = useTranslation();
    return (
        <div
            style={{
                position: "fixed",
                left: position.x,
                top: position.y,
            }}
        >
            <ContextMenu
                contextOverlayProps={{
                    onClose,
                    defaultIsOpen: true,
                    autoFocus: true,
                    interactionKind: "hover",
                }}
                togglerElement={<div />}
            >
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
            </ContextMenu>
        </div>
    );
};
