import React from "react";
import { FieldItem, Icon, IconButton, Spacing, TextField, Toolbar, ToolbarSection } from "@eccenca/gui-elements";
import { useTranslation } from "react-i18next";
import { ParameterCallbacks } from "./ParameterWidget";

interface Props {
    // ID of the parameter
    parameterId: string;
    // Label of the parameter
    label: string;
    // If required "(required)" will be placed next to the label
    required?: boolean;
    // Error message that will be displayed below the input component
    errorMessage?: string;
    // The input element
    children: JSX.Element;
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
        parameterCallbacks: ParameterCallbacks;
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
    children,
    helperText,
    disabled = false,
    tooltip,
    supportVariableTemplateElement,
}: Props) => {
    const [t] = useTranslation();
    const [showVariableTemplateInput, setShowVariableTemplateInput] = React.useState(false);
    const startWithTemplateView = supportVariableTemplateElement?.startWithTemplateView ?? false;
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
                            onChange={(e) => supportVariableTemplateElement!.onChange(e.target.value)}
                        />
                    ) : (
                        children
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
