import React from "react";
import { EdgeTools } from "gui-elements/src/extensions/react-flow";
import { XYPosition } from "react-flow-renderer/dist/types";
import { Button } from "gui-elements";
import { useTranslation } from "react-i18next";

interface SelectionMenuProps {
    /** Position of the menu. */
    position: XYPosition;
    /** Called when menu should be closed. */
    onClose: () => any;
    /** Callback to remove edge. */
    removeSelection: () => any;
}

/** Rule edge menu. */
export const SelectionMenu = ({ position, onClose, removeSelection }: SelectionMenuProps) => {
    const [t] = useTranslation();
    return (
        // TODO CMEM-4080: Use a generic "tools" component or rename EdgeTools
        <EdgeTools posOffset={{ left: position.x, top: position.y }} onClose={onClose}>
            <Button
                minimal
                icon="item-remove"
                data-test-id={"selection-menu-remove-btn"}
                tooltip={t("RuleEditor.selection.menu.delete.tooltip")}
                tooltipProperties={{
                    autoFocus: false,
                    enforceFocus: false,
                    openOnTargetFocus: false,
                }}
                small
                onClick={() => {
                    onClose();
                    removeSelection();
                }}
            >
                {t("RuleEditor.selection.menu.delete.label")}
            </Button>
        </EdgeTools>
    );
};
