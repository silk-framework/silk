import {
    AutoSuggestion,
    FieldItem,
    IconButton,
    Spacing,
    TextField,
    Toolbar,
    ToolbarSection,
} from "@eccenca/gui-elements";
import React, { MouseEventHandler, MutableRefObject } from "react";
import { useTranslation } from "react-i18next";
import {
    ValidateTemplateResponse,
    requestValidateTemplateString,
} from "../modals/CreateArtefactModal/CreateArtefactModal.requests";
import { requestAutoCompleteTemplateString } from "../modals/CreateArtefactModal/CreateArtefactModal.requests";
import { IValidationResult } from "@eccenca/gui-elements/src/components/AutoSuggestion/AutoSuggestion";
import useErrorHandler from "../../../hooks/useErrorHandler";
import { ValueStateRef } from "../VariablesWidget/VariableModal";

interface TemplateValueInputProps {
    disabled?: boolean;
    helperText?: string;
    messageText?: string;
    hasStateDanger?: boolean;
    projectId: string;
}

const TemplateValueInput = React.forwardRef(
    (
        { disabled, helperText, projectId, hasStateDanger, messageText }: TemplateValueInputProps,
        valueStateRef: MutableRefObject<ValueStateRef>
    ) => {
        const [showVariableTemplateInput, setShowVariableTemplateInput] = React.useState<boolean>(false);
        const [validationError, setValidationError] = React.useState<string>();
        const [templateInfoMessage, setTemplateInfoMessage] = React.useState<string>();
        const [showRareActions, setShowRareActions] = React.useState(false);
        const showRareElementState = React.useRef<{ timeout?: number }>({});
        const [t] = useTranslation();

        const switchShowVariableTemplateInput = React.useCallback(() => {
            setShowVariableTemplateInput((old) => {
                const becomesTemplate = !old;
                //false  means currently input to become template
                if (becomesTemplate) {
                    valueStateRef.current.inputValueBeforeSwitch = valueStateRef.current.currentInputValue;
                } else {
                    valueStateRef.current.templateValueBeforeSwitch = valueStateRef.current.currentTemplateValue;
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
                            />
                        ) : (
                            <TextField
                                id="value"
                                intent={!!messageText ? "danger" : "none"}
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

interface TemplateInputComponentProps {
    initialValue: string;
    onTemplateValueChange: (any) => any;
    setValidationError: (error?: string) => any;
    /** Called with a message that contains the currently evaluated template. */
    evaluatedValueMessage?: (evaluatedTemplateMessage?: string) => any;
    projectId: string;
}

const TemplateInputComponent = React.memo(
    ({
        initialValue,
        onTemplateValueChange,
        setValidationError,
        evaluatedValueMessage,
        projectId,
    }: TemplateInputComponentProps) => {
        const { registerError } = useErrorHandler();
        const [t] = useTranslation();

        const processValidationError = React.useCallback((validationResult: IValidationResult): IValidationResult => {
            let errorMessage = validationResult.parseError?.message;
            const adaptedValidationResult = { ...validationResult };
            if (errorMessage) {
                if (errorMessage.includes("error at position")) {
                    // Parse position from error message
                    const result = /error at position (\d+)/.exec(errorMessage);
                    if (result && Number.isInteger(Number.parseInt(result[1]))) {
                        const errorPosition = Number.parseInt(result[1]);
                        adaptedValidationResult.parseError = {
                            start: errorPosition,
                            end: errorPosition + 2,
                            message: validationResult.parseError!.message,
                        };
                        errorMessage = `${t("ArtefactFormParameter.invalidTemplate")}: ${errorMessage}`;
                    }
                }
            }
            setValidationError(errorMessage);
            return adaptedValidationResult;
        }, []);

        const autoComplete = React.useCallback(async (inputString: string, cursorPosition: number) => {
            try {
                return (await requestAutoCompleteTemplateString(inputString, cursorPosition, projectId)).data;
            } catch (error) {
                registerError("ArtefactFormParameter.autoComplete", "Auto-completing the template has failed.", error);
            }
        }, []);

        const checkTemplate = React.useCallback(
            async (inputString: string): Promise<ValidateTemplateResponse | undefined> => {
                try {
                    const validationResponse = (await requestValidateTemplateString(inputString, projectId)).data;
                    evaluatedValueMessage?.(
                        validationResponse.evaluatedTemplate
                            ? t("ArtefactFormParameter.evaluatedValue", { value: validationResponse.evaluatedTemplate })
                            : undefined
                    );
                    return processValidationError(validationResponse);
                } catch (error) {
                    registerError("ArtefactFormParameter.checkTemplate", "Validating template has failed.", error);
                    evaluatedValueMessage?.(undefined);
                }
            },
            [processValidationError]
        );

        return (
            <>
                <AutoSuggestion
                    initialValue={initialValue}
                    onChange={onTemplateValueChange}
                    fetchSuggestions={autoComplete}
                    checkInput={checkTemplate}
                    autoCompletionRequestDelay={200}
                />
            </>
        );
    }
);

export default TemplateValueInput;
