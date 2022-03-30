import { Button, SimpleDialog } from "gui-elements";
import React from "react";
import { RuleNodeParameterForm, RuleNodeParametersProps } from "./RuleNodeParameterForm";
import { useTranslation } from "react-i18next";

interface RuleNodeFormParameterModalProps extends Omit<RuleNodeParametersProps, "large"> {
    onClose: () => any;
    /** Title of the modal */
    title: string;
}

/** Modal to edit rule node parameters. */
export const RuleNodeFormParameterModal = ({
    onClose,
    title,
    ...ruleNodeParameterProps
}: RuleNodeFormParameterModalProps) => {
    const [t] = useTranslation();
    return (
        <SimpleDialog
            data-test-id={"rule-node-parameter-form-modal"}
            size={"large"}
            hasBorder
            isOpen={true}
            title={title}
            onClose={onClose}
            actions={[
                <Button key="cancel" onClick={onClose}>
                    {t("common.action.close")}
                </Button>,
            ]}
            wrapperDivProps={{
                // Prevent react-flow from getting these events
                onContextMenu: (event) => event.stopPropagation(),
                onDrag: (event) => event.stopPropagation(),
                onDragStart: (event) => event.stopPropagation(),
                onDragEnd: (event) => event.stopPropagation(),
                onMouseDown: (event) => event.stopPropagation(),
                onMouseUp: (event) => event.stopPropagation(),
                onClick: (event) => event.stopPropagation(),
            }}
        >
            <RuleNodeParameterForm {...ruleNodeParameterProps} large={true} />
        </SimpleDialog>
    );
};
