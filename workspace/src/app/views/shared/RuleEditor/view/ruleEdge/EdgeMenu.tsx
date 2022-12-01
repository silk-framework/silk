import { Button } from "@eccenca/gui-elements";
import { EdgeTools } from "@eccenca/gui-elements/src/extensions/react-flow";
import React from "react";
import { XYPosition } from "react-flow-renderer/dist/types";
import { useTranslation } from "react-i18next";

interface EdgeMenuProps {
    /** Position of the menu. */
    position: XYPosition;
    /** Called when menu should be closed. */
    onClose: () => any;
    /** Callback to remove edge. */
    removeEdge: () => any;
}

/** Rule edge menu. */
export const EdgeMenu = ({ position, onClose, removeEdge }: EdgeMenuProps) => {
    const [t] = useTranslation();
    return (
        <EdgeTools posOffset={{ left: position.x, top: position.y }} onClose={onClose}>
            <Button
                minimal
                icon="item-remove"
                data-test-id={"edge-menu-remove-btn"}
                tooltip={t("RuleEditor.edge.menu.delete.tooltip")}
                tooltipProps={{
                    autoFocus: false,
                    enforceFocus: false,
                    openOnTargetFocus: false,
                }}
                small
                onClick={() => {
                    onClose();
                    removeEdge();
                }}
            >
                {t("RuleEditor.edge.menu.delete.label")}
            </Button>
        </EdgeTools>
    );
};
