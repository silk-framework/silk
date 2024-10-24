import { SimpleDialogProps } from "@eccenca/gui-elements/src/components/Dialog/SimpleDialog";
import { SimpleDialog } from "@eccenca/gui-elements";
import React, { useEffect } from "react";
import { RuleEditorUiContext } from "../../contexts/RuleEditorUiContext";
import { ReactFlowHotkeyContext } from "@eccenca/gui-elements/src/cmem/react-flow/extensions/ReactFlowHotkeyContext";

interface IProps extends SimpleDialogProps {}

/** Base modal for every modal that is used inside the rule editor.
 * This will prevent certain hot keys and events from having an effect in the editor. */
export const RuleEditorBaseModal = ({ children, ...props }: IProps) => {
    const ruleEditorUiContext = React.useContext(RuleEditorUiContext);
    const { disableHotKeys } = React.useContext(ReactFlowHotkeyContext);

    React.useEffect(() => {
        disableHotKeys(props.isOpen);

        return () => {
            disableHotKeys(false);
        };
    }, [props.isOpen]);

    // Enable editor flag "modal shown" flag
    useEffect(() => {
        if (props.isOpen) {
            ruleEditorUiContext.setModalShown(true);
            return () => ruleEditorUiContext.setModalShown(false);
        }
    }, [props.isOpen]);

    return <SimpleDialog {...props}>{children}</SimpleDialog>;
};
