import React from "react";
import { EdgeTools } from "@eccenca/gui-elements/src/extensions/react-flow";
import { XYPosition } from "react-flow-renderer/dist/types";
import { Button, Spacing } from "@eccenca/gui-elements";
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
            <Button
                minimal
                icon="item-remove"
                data-test-id={"selection-menu-remove-btn"}
                tooltip={t("RuleEditor.selection.menu.delete.tooltip")}
                tooltipProps={{
                    autoFocus: false,
                    enforceFocus: false,
                    openOnTargetFocus: false,
                }}
                small
                hasStateDanger
                onClick={() => {
                    onClose();
                    removeSelection();
                }}
            >
                {t("RuleEditor.selection.menu.delete.label")}
            </Button>
            <Spacing size="tiny" />
            <Button
                minimal
                icon="item-clone"
                data-test-id={"selection-menu-clone-btn"}
                tooltip={t("RuleEditor.selection.menu.clone.tooltip")}
                tooltipProps={{
                    autoFocus: false,
                    enforceFocus: false,
                    openOnTargetFocus: false,
                }}
                small
                onClick={() => {
                    onClose();
                    cloneSelection();
                }}
            >
                {t("RuleEditor.selection.menu.clone.label")}
            </Button>
            <Spacing size="tiny" />
            <Button
                minimal
                icon="item-copy"
                data-test-id={"selection-menu-copy-btn"}
                tooltip={t("RuleEditor.selection.menu.copy.tooltip")}
                tooltipProps={{
                    autoFocus: false,
                    enforceFocus: false,
                    openOnTargetFocus: false,
                }}
                small
                onClick={() => {
                    onClose();
                    copySelection();
                }}
            >
                {t("RuleEditor.selection.menu.copy.label")}
            </Button>
        </EdgeTools>
    );
};
