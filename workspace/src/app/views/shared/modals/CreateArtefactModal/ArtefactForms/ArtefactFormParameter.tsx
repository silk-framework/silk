import React from "react";
import { FieldItem, Icon, IconButton, Spacing, TextField, Toolbar, ToolbarSection } from "@eccenca/gui-elements";
import { useTranslation } from "react-i18next";
import { ExtendedParameterCallbacks } from "./ParameterWidget";

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
     */
    inputElementFactory: (initialValueReplace?: string, onChange?: (value: string) => any) => JSX.Element;
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
    const [showVariableTemplateInput, setShowVariableTemplateInput] = React.useState(startWithTemplateView);
    const initialValue = supportVariableTemplateElement?.initialValue ?? "";
    const valueState = React.useRef({
        inputValue: startWithTemplateView ? "" : initialValue,
        templateValue: startWithTemplateView ? initialValue : "",
    });
    const switchShowVariableTemplateInput = React.useCallback(() => {
        setShowVariableTemplateInput((old) => {
            const becomesTemplate = !old;
            supportVariableTemplateElement!.parameterCallbacks.setTemplateFlag(parameterId, becomesTemplate);
            supportVariableTemplateElement!.onChange(
                becomesTemplate ? valueState.current.templateValue : valueState.current.inputValue
            );
            return becomesTemplate;
        });
    }, []);

    const onTemplateValueChange = React.useCallback(
        (e) => {
            const value = e.target.value;
            valueState.current.templateValue = value;
            supportVariableTemplateElement!.onChange(value);
        },
        [supportVariableTemplateElement?.onChange]
    );

    const onElementValueChange = React.useCallback(
        (valueOrEvent: any) => {
            const value = valueOrEvent.target ? valueOrEvent.target.value : `${valueOrEvent}`;
            valueState.current.inputValue = value;
            supportVariableTemplateElement?.onChange(value);
        },
        [supportVariableTemplateElement?.onChange]
    );

    return (
        <FieldItem
            key={parameterId}
            labelProps={{
                text: label,
                info: required ? t("common.words.required") : undefined,
                htmlFor: parameterId,
                tooltip: tooltip,
            }}
            hasStateDanger={!!errorMessage}
            messageText={errorMessage}
            disabled={disabled}
            helperText={helperText}
        >
            <Toolbar>
                <ToolbarSection canGrow={true}>
                    {supportVariableTemplateElement && showVariableTemplateInput ? (
                        <TextField
                            // TODO Better distinguish template input field
                            leftElement={<Icon name={"item-edit"} />}
                            defaultValue={valueState.current.templateValue}
                            onChange={onTemplateValueChange}
                        />
                    ) : (
                        inputElementFactory(valueState.current.inputValue, onElementValueChange)
                    )}
                </ToolbarSection>
                {supportVariableTemplateElement && !disabled && (
                    <ToolbarSection>
                        <Spacing vertical={true} size={"small"} />
                        <IconButton
                            fill={false}
                            name={showVariableTemplateInput ? "navigation-back" : "navigation-next"} // TODO: Find good icon
                            data-test-id={`${parameterId}-template-switch-btn`}
                            onClick={switchShowVariableTemplateInput}
                        />
                    </ToolbarSection>
                )}
            </Toolbar>
        </FieldItem>
    );
};
