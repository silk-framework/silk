import React, { memo, MouseEventHandler } from "react";
import { AutoSuggestion, FieldItem, Icon, IconButton, Spacing, Toolbar, ToolbarSection } from "@eccenca/gui-elements";
import { useTranslation } from "react-i18next";
import { ExtendedParameterCallbacks } from "./ParameterWidget";
import { requestValidateTemplateString, ValidateTemplateResponse } from "../CreateArtefactModal.requests";
import useErrorHandler from "../../../../../hooks/useErrorHandler";

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
        switchButtonPosition: "rightElement";
        startWithTemplateView: boolean;
        parameterCallbacks: ExtendedParameterCallbacks;
        // The initial value. An undefined value means, it is unknown.
        initialValue: string;
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
    const initialValue = supportVariableTemplateElement?.initialValue;
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
        currentInputValue: startWithTemplateView ? undefined : initialValue,
        currentTemplateValue: startWithTemplateView ? initialValue ?? "" : "",
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
            supportVariableTemplateElement!.parameterCallbacks.setTemplateFlag(parameterId, becomesTemplate);
            supportVariableTemplateElement!.onChange(
                becomesTemplate ? valueState.current.currentTemplateValue : valueState.current.currentInputValue ?? ""
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
            const value = valueOrEvent.target ? valueOrEvent.target.value : `${valueOrEvent}`;
            valueState.current.currentInputValue = value;
            supportVariableTemplateElement?.onChange(value);
        },
        [supportVariableTemplateElement?.onChange]
    );
    const onMouseOver: MouseEventHandler<HTMLDivElement> = React.useCallback(() => {
        if (showRareElementState.current.timeout != null) {
            clearTimeout(showRareElementState.current.timeout);
        }
        setShowRareActions(true);
    }, []);
    const onMouseOut: MouseEventHandler<HTMLDivElement> = React.useCallback(() => {
        showRareElementState.current.timeout = window.setTimeout(() => setShowRareActions(false), 50);
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
            messageText={errorMessage || validationError}
            disabled={disabled}
            helperText={helperText}
        >
            <Toolbar
                onMouseOver={showVariableTemplateInput ? undefined : onMouseOver}
                onMouseOut={showVariableTemplateInput ? undefined : onMouseOut}
            >
                <ToolbarSection canGrow={true}>
                    {supportVariableTemplateElement && showVariableTemplateInput ? (
                        <TemplateInputComponent
                            parameterId={parameterId}
                            initialValue={valueState.current.templateValueBeforeSwitch ?? ""}
                            onTemplateValueChange={onTemplateValueChange}
                            setValidationError={setValidationError}
                        />
                    ) : (
                        inputElementFactory(valueState.current.inputValueBeforeSwitch, onElementValueChange)
                    )}
                </ToolbarSection>
                {supportVariableTemplateElement && !disabled && showSwitchButton && (
                    <ToolbarSection>
                        <Spacing vertical={true} size={"small"} />
                        <IconButton
                            fill={false}
                            tooltipProps={{
                                hoverOpenDelay: 50,
                                placement: "top",
                            }}
                            text={
                                showVariableTemplateInput
                                    ? t("ArtefactFormParameter.switchToValue")
                                    : t("ArtefactFormParameter.switchToTemplate")
                            }
                            name={showVariableTemplateInput ? "navigation-back" : "navigation-next"} // TODO: Find good icon
                            data-test-id={`${parameterId}-template-switch-${
                                showVariableTemplateInput ? "back" : "to"
                            }-btn`}
                            onClick={switchShowVariableTemplateInput}
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
}

const TemplateInputComponent = memo(
    ({ parameterId, initialValue, onTemplateValueChange, setValidationError }: TemplateInputComponentProps) => {
        const { registerError } = useErrorHandler();
        // TODO: implement
        const autoComplete = React.useCallback(() => undefined, []);
        const templateIcon = React.useMemo(() => <Icon name={"item-edit"} />, []);

        const checkTemplate = React.useCallback(
            async (inputString: string): Promise<ValidateTemplateResponse | undefined> => {
                try {
                    const validationResponse = (await requestValidateTemplateString(inputString)).data;
                    setValidationError(validationResponse.parseError?.message);
                    return validationResponse;
                } catch (error) {
                    registerError("ArtefactFormParameter.checkTemplate", "Validating template has failed.", error);
                }
            },
            []
        );

        return (
            <AutoSuggestion
                id={parameterId}
                // TODO Better distinguish template input field
                leftElement={templateIcon}
                initialValue={initialValue}
                onChange={onTemplateValueChange}
                fetchSuggestions={autoComplete}
                checkInput={checkTemplate}
            />
        );
    }
);
