import React, { memo, MouseEventHandler } from "react";
import { AutoSuggestion, FieldItem, IconButton, Spacing, Toolbar, ToolbarSection } from "@eccenca/gui-elements";
import { useTranslation } from "react-i18next";
import { ExtendedParameterCallbacks } from "./ParameterWidget";
import {
    requestAutoCompleteTemplateString,
    requestValidateTemplateString,
    ValidateTemplateResponse,
} from "../CreateArtefactModal.requests";
import useErrorHandler from "../../../../../hooks/useErrorHandler";
import {
    LabelledParameterValue,
    OptionallyLabelledParameter,
    optionallyLabelledParameterToValue,
} from "../../../../taskViews/linking/linking.types";
import { IValidationResult } from "@eccenca/gui-elements/src/components/AutoSuggestion/AutoSuggestion";
import { useSelector } from "react-redux";
import { commonSel } from "@ducks/common";

interface Props {
    // ID of the parameter
    parameterId: string;
    // Label of the parameter
    label: string;
    // If required "(required)" will be placed next to the label
    required?: boolean;
    // Error message that will be displayed below the input component
    errorMessage?: string;
    /** Factory to create the input element
     *
     * @param initialValueReplace If defined, the value the element should be initialized with instead of the starting initial value.
     * @param onChange     onChange function from supportVariableTemplateElement.onChange
     *                     NOTE: Do not use this when not using supportVariableTemplateElement at the same time!
     * @param showRareActions Callback when "rare" actions should be shown, e.g. the variable template switch button. TODO
     */
    inputElementFactory: (
        initialValueReplace?: string,
        onChange?: (value: string) => any,
        showRareActions?: (show: boolean) => any
    ) => JSX.Element;
    // If the parameter is disabled
    disabled?: boolean;
    // Is displayed between label and input element.
    helperText?: string | JSX.Element;
    // Tooltip to display
    tooltip?: string;
    /* If defined, the parameter supports variable templates that gets their values set in the backend with variable substitution.
     * The input can be switched when the user requests it.
     */
    supportVariableTemplateElement?: {
        onChange: (value: string) => any;
        startWithTemplateView: boolean;
        // If the evaluated template should be shown below the template input
        showTemplatePreview: boolean;
        parameterCallbacks: ExtendedParameterCallbacks;
        // The initial value. An undefined value means, it is unknown.
        initialValue: string | number | boolean | OptionallyLabelledParameter<string | number | boolean>;
        // The default value of the normal input
        defaultValue?: string | number | boolean | OptionallyLabelledParameter<string | number | boolean>;
    };
}

/** Wrapper around the input element of a parameter. Supports switching to variable templates. */
export const ArtefactFormParameter = ({
    parameterId,
    label,
    required = false,
    errorMessage,
    inputElementFactory,
    helperText,
    disabled = false,
    tooltip,
    supportVariableTemplateElement,
}: Props) => {
    const [t] = useTranslation();
    const startWithTemplateView = supportVariableTemplateElement?.startWithTemplateView ?? false;
    const [showVariableTemplateInput, setShowVariableTemplateInput] = React.useState<boolean>(startWithTemplateView);
    const [showRareActions, setShowRareActions] = React.useState(false);
    const [validationError, setValidationError] = React.useState<string | undefined>(undefined);
    const [templateInfoMessage, setTemplateInfoMessage] = React.useState<string | undefined>(undefined);
    const { templatingEnabled } = useSelector(commonSel.initialSettingsSelector);
    let initialValue: any = supportVariableTemplateElement?.initialValue;
    // Initial value might be a labelled value, safe-guard
    if (initialValue != null && (initialValue as OptionallyLabelledParameter<any>)?.value != null) {
        initialValue = `${(initialValue as OptionallyLabelledParameter<any>).value}`;
    }
    const stringDefaultValue: string | LabelledParameterValue<any> | undefined =
        typeof supportVariableTemplateElement?.defaultValue === "object"
            ? // The auto-completion values should be kept as is
              supportVariableTemplateElement?.defaultValue
            : supportVariableTemplateElement?.defaultValue != null
            ? // Other defaults should be converted to string
              `${supportVariableTemplateElement?.defaultValue}`
            : undefined;
    const valueState = React.useRef<{
        // The most recent value of the input component
        currentInputValue?: string;
        // The last input value before the switch happened from input -> template
        inputValueBeforeSwitch?: string;
        // The most recent template value
        currentTemplateValue: string;
        // The last template value before the switch happened from template -> input
        templateValueBeforeSwitch?: string;
    }>({
        // Input value needs to be undefined, so it gets set to the default value
        currentInputValue: startWithTemplateView ? stringDefaultValue : initialValue,
        currentTemplateValue: startWithTemplateView ? initialValue : undefined,
    });
    const showRareElementState = React.useRef<{ timeout?: number }>({});
    const switchShowVariableTemplateInput = React.useCallback(() => {
        setShowVariableTemplateInput((old) => {
            const becomesTemplate = !old;
            if (becomesTemplate) {
                valueState.current.inputValueBeforeSwitch = valueState.current.currentInputValue;
            } else {
                valueState.current.templateValueBeforeSwitch = valueState.current.currentTemplateValue;
            }
            setValidationError(undefined);
            setTemplateInfoMessage(undefined);
            supportVariableTemplateElement!.parameterCallbacks.setTemplateFlag(parameterId, becomesTemplate);
            supportVariableTemplateElement!.onChange(
                becomesTemplate
                    ? valueState.current.currentTemplateValue
                    : optionallyLabelledParameterToValue(valueState.current.currentInputValue) ?? ""
            );
            return becomesTemplate;
        });
    }, []);

    const onTemplateValueChange = React.useCallback(
        (e) => {
            const value = e.target ? e.target.value : e;
            valueState.current.currentTemplateValue = value;
            supportVariableTemplateElement!.onChange(value);
        },
        [supportVariableTemplateElement?.onChange]
    );

    const onElementValueChange = React.useCallback(
        (valueOrEvent: any) => {
            const value = valueOrEvent.target ? `${valueOrEvent.target.value}` : `${valueOrEvent}`;
            valueState.current.currentInputValue = value;
            supportVariableTemplateElement?.onChange(value);
        },
        [supportVariableTemplateElement?.onChange]
    );
    const onMouseOver: MouseEventHandler<HTMLDivElement> = React.useCallback(() => {
        if (showRareElementState.current.timeout != null) {
            clearTimeout(showRareElementState.current.timeout);
        }
        showRareElementState.current.timeout = window.setTimeout(() => setShowRareActions(true), 50);
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
            key={parameterId}
            labelProps={{
                text: label,
                info: required ? t("common.words.required") : undefined,
                htmlFor: parameterId,
                tooltip: tooltip,
            }}
            hasStateDanger={!!errorMessage || !!validationError}
            messageText={errorMessage || validationError || templateInfoMessage}
            disabled={disabled}
            helperText={helperText}
        >
            <Toolbar
                onMouseOver={showVariableTemplateInput ? undefined : onMouseOver}
                onMouseOut={showVariableTemplateInput ? undefined : onMouseOut}
                style={{ alignItems: "flex-start" }}
            >
                <ToolbarSection canGrow={true} style={{ alignSelf: "center" }}>
                    {supportVariableTemplateElement && showVariableTemplateInput ? (
                        <TemplateInputComponent
                            parameterId={parameterId}
                            initialValue={
                                valueState.current.templateValueBeforeSwitch ??
                                valueState.current.inputValueBeforeSwitch ??
                                initialValue ??
                                ""
                            }
                            onTemplateValueChange={onTemplateValueChange}
                            setValidationError={setValidationError}
                            evaluatedValueMessage={
                                supportVariableTemplateElement.showTemplatePreview ? setTemplateInfoMessage : undefined
                            }
                        />
                    ) : (
                        inputElementFactory(valueState.current.inputValueBeforeSwitch, onElementValueChange)
                    )}
                </ToolbarSection>
                {templatingEnabled && supportVariableTemplateElement && !disabled && (
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
                            data-test-id={`${parameterId}-template-switch-${
                                showVariableTemplateInput ? "back" : "to"
                            }-btn`}
                            onClick={switchShowVariableTemplateInput}
                            minimal={false}
                            outlined
                            hasStatePrimary={showVariableTemplateInput}
                            active={showVariableTemplateInput}
                        />
                    </ToolbarSection>
                )}
            </Toolbar>
        </FieldItem>
    );
};

interface TemplateInputComponentProps {
    parameterId: string;
    initialValue: string;
    onTemplateValueChange: (any) => any;
    setValidationError: (error?: string) => any;
    /** Called with a message that contains the currently evaluated template. */
    evaluatedValueMessage?: (evaluatedTemplateMessage?: string) => any;
}

const TemplateInputComponent = memo(
    ({
        parameterId,
        initialValue,
        onTemplateValueChange,
        setValidationError,
        evaluatedValueMessage,
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
                return (await requestAutoCompleteTemplateString(inputString, cursorPosition)).data;
            } catch (error) {
                registerError("ArtefactFormParameter.autoComplete", "Auto-completing the template has failed.", error);
            }
        }, []);

        const checkTemplate = React.useCallback(
            async (inputString: string): Promise<ValidateTemplateResponse | undefined> => {
                try {
                    const validationResponse = (await requestValidateTemplateString(inputString)).data;
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
                    id={parameterId}
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