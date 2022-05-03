import { SimpleDialogProps } from "@eccenca/gui-elements/src/components/Dialog/SimpleDialog";
import { SimpleDialog } from "@eccenca/gui-elements";
import React, { useEffect } from "react";
import { RuleEditorUiContext } from "../../contexts/RuleEditorUiContext";

interface IProps extends SimpleDialogProps {}

/** Base modal for every modal that is used inside the rule editor.
 * This will prevent certain hot keys and events from having an effect in the editor. */
export const RuleEditorBaseModal = ({ children, ...props }: IProps) => {
    const ruleEditorUiContext = React.useContext(RuleEditorUiContext);

    // Enable editor flag "modal shown" flag
    useEffect(() => {
        if (props.isOpen) {
            ruleEditorUiContext.setModalShown(true);
            return () => ruleEditorUiContext.setModalShown(false);
        }
    }, [props.isOpen]);

    const wrapperDivProps = {
        // Prevent react-flow from getting these events
        onContextMenu: (event) => event.stopPropagation(),
        onDrag: (event) => event.stopPropagation(),
        onDragStart: (event) => event.stopPropagation(),
        onDragEnd: (event) => event.stopPropagation(),
        onMouseDown: (event) => event.stopPropagation(),
        onMouseUp: (event) => event.stopPropagation(),
        onClick: (event) => event.stopPropagation(),
        ...props.wrapperDivProps,
    };

    return (
        <SimpleDialog {...props} wrapperDivProps={wrapperDivProps}>
            {children}
        </SimpleDialog>
    );
};
