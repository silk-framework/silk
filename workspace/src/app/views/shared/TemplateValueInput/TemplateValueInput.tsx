import { FieldItem, IconButton, Spacing, TextField, Toolbar, ToolbarSection } from "@eccenca/gui-elements";
import React, { MouseEventHandler, MutableRefObject } from "react";
import { useTranslation } from "react-i18next";
import { ValueStateRef } from "../VariablesWidget/modals/NewVariableModal";
import { TemplateInputComponent } from "../modals/CreateArtefactModal/ArtefactForms/ArtefactFormParameter";

interface TemplateValueInputProps {
    disabled?: boolean;
    helperText?: string;
    messageText?: string;
    hasStateDanger?: boolean;
    projectId: string;
    /** ID of the input component. */
    parameterId?: string;
}

const TemplateValueInput = React.forwardRef(
    (
        {
            disabled,
            helperText,
            projectId,
            hasStateDanger,
            messageText,
            parameterId = "template-value-input",
        }: TemplateValueInputProps,
        valueStateRef: MutableRefObject<ValueStateRef>
    ) => {
        const [showVariableTemplateInput, setShowVariableTemplateInput] = React.useState<boolean>(false);
        const [validationError, setValidationError] = React.useState<string>();
        const [templateInfoMessage, setTemplateInfoMessage] = React.useState<string>();
        const [showRareActions, setShowRareActions] = React.useState(false);
        const showRareElementState = React.useRef<{ timeout?: number }>({});
        const [t] = useTranslation();

        React.useEffect(() => {
            setShowVariableTemplateInput(!!valueStateRef.current.templateValueBeforeSwitch);
        }, [valueStateRef.current.templateValueBeforeSwitch]);

        const switchShowVariableTemplateInput = React.useCallback(() => {
            setShowVariableTemplateInput((old) => {
                const becomesTemplate = !old;
                //false  means currently input to become template
                if (becomesTemplate) {
                    valueStateRef.current.inputValueBeforeSwitch =
                        valueStateRef.current.currentInputValue || valueStateRef.current.inputValueBeforeSwitch;
                } else {
                    valueStateRef.current.templateValueBeforeSwitch =
                        valueStateRef.current.currentTemplateValue || valueStateRef.current.currentTemplateValue;
                }
                setValidationError(undefined);
                setTemplateInfoMessage(undefined);
                return !old;
            });
        }, []);

        const onTemplateValueChange = React.useCallback((e) => {
            const val = e.target ? e.target.value : e;
            valueStateRef.current.currentTemplateValue = val;
        }, []);

        const onElementValueChange = React.useCallback((valueOrEvent: any) => {
            const val = valueOrEvent.target ? `${valueOrEvent.target.value}` : `${valueOrEvent}`;
            valueStateRef.current.currentInputValue = val;
        }, []);

        const onMouseOver: MouseEventHandler<HTMLDivElement> = React.useCallback(() => {
            if (showRareElementState.current.timeout != null) {
                clearTimeout(showRareElementState.current.timeout);
            }
            showRareElementState.current.timeout = window.setTimeout(() => setShowRareActions(true), 150);
        }, []);
        const onMouseOut: MouseEventHandler<HTMLDivElement> = React.useCallback(() => {
            if (showRareElementState.current.timeout != null) {
                clearTimeout(showRareElementState.current.timeout);
            }
            showRareElementState.current.timeout = window.setTimeout(() => setShowRareActions(false), 250);
        }, []);

        const showSwitchButton = showRareActions || showVariableTemplateInput; // always show for variable templates

        return (
            <FieldItem
                labelProps={{
                    htmlFor: "value",
                    text: "Value",
                }}
                hasStateDanger={hasStateDanger || !!validationError}
                messageText={messageText || validationError || templateInfoMessage}
                disabled={disabled}
                helperText={helperText}
            >
                <Toolbar
                    onMouseOver={showVariableTemplateInput ? undefined : onMouseOver}
                    onMouseOut={showVariableTemplateInput ? undefined : onMouseOut}
                    style={{ alignItems: "flex-start" }}
                    noWrap
                >
                    <ToolbarSection
                        canGrow
                        style={{
                            alignSelf: "center",
                            maxWidth: showVariableTemplateInput ? "calc(100% - 3.5px - 32px)" : "auto", // set full width minus tiny spacing and icon button width
                        }}
                    >
                        {showVariableTemplateInput ? (
                            <TemplateInputComponent
                                initialValue={valueStateRef.current.templateValueBeforeSwitch ?? ""}
                                onTemplateValueChange={onTemplateValueChange}
                                setValidationError={setValidationError}
                                evaluatedValueMessage={setTemplateInfoMessage}
                                projectId={projectId}
                                parameterId={parameterId}
                            />
                        ) : (
                            <TextField
                                id="value"
                                intent={!!messageText ? "danger" : "none"}
                                key={valueStateRef.current.inputValueBeforeSwitch}
                                defaultValue={valueStateRef.current.inputValueBeforeSwitch}
                                onChange={onElementValueChange}
                            />
                        )}
                    </ToolbarSection>
                    <ToolbarSection hideOverflow style={!showSwitchButton ? { width: "0px" } : {}}>
                        <Spacing vertical={true} size={"tiny"} />
                        <IconButton
                            fill={false}
                            tooltipProps={{
                                hoverOpenDelay: 50,
                                placement: "top",
                            }}
                            text={
                                showVariableTemplateInput
                                    ? t("ArtefactFormParameter.switchToValue").replace("EXAMPLE", "{{global.myVar}}")
                                    : t("ArtefactFormParameter.switchToTemplate").replace("EXAMPLE", "{{global.myVar}}")
                            }
                            name={"form-template"}
                            onClick={switchShowVariableTemplateInput}
                            minimal={false}
                            outlined
                            hasStatePrimary={showVariableTemplateInput}
                            active={showVariableTemplateInput}
                        />
                    </ToolbarSection>
                </Toolbar>
            </FieldItem>
        );
    }
);

export default TemplateValueInput;
