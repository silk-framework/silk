import { IRuleNodeParameter } from "./RuleNodeParameter.typings";
import React, { MouseEvent } from "react";
import { CodeEditor, Switch, TextField } from "@eccenca/gui-elements";
import { requestResourcesList } from "@ducks/shared/requests";
import { Intent } from "@blueprintjs/core";
import { useTranslation } from "react-i18next";
import { RuleEditorContext } from "../../contexts/RuleEditorContext";
import { ruleEditorNodeParameterValue } from "../../model/RuleEditorModel.typings";
import { RuleEditorModelContext } from "../../contexts/RuleEditorModelContext";
import useErrorHandler from "../../../../../hooks/useErrorHandler";
import { SelectFileFromExisting } from "../../../FileUploader/cases/SelectFileFromExisting";
import { ParameterAutoCompletion } from "../../../modals/CreateArtefactModal/ArtefactForms/ParameterAutoCompletion";
import { IOperatorNodeParameterValueWithLabel } from "../../../../taskViews/shared/rules/rule.typings";
import { fileValue, IProjectResource } from "@ducks/shared/typings";
import { TextFieldWithCharacterWarnings } from "../../../extendedGuiElements/TextFieldWithCharacterWarnings";
import { TextAreaWithCharacterWarnings } from "../../../extendedGuiElements/TextAreaWithCharacterWarnings";

interface RuleParameterInputProps {
    /** ID of the plugin this parameter is part of. */
    pluginId: string;
    /** The parameter config. */
    ruleParameter: IRuleNodeParameter;
    /** The ID of the node the parameter is part of. */
    nodeId: string;
    /** If there should be error highlighting of the input component. */
    hasValidationError: boolean;
    /** Requests values of parameters this parameter might depend on for auto-completion. */
    dependentValue: (paramId: string) => string | undefined;
    /** If the form parameter will be rendered in a large area. The used input components might differ. */
    large: boolean;
    /** When used inside a modal, the behavior of some components will be optimized. */
    insideModal: boolean;
}

/** An input widget for a parameter value. */
export const RuleParameterInput = ({
    pluginId,
    ruleParameter,
    nodeId,
    hasValidationError,
    dependentValue,
    large,
    insideModal,
}: RuleParameterInputProps) => {
    const onChange = ruleParameter.update;
    const ruleEditorContext = React.useContext(RuleEditorContext);
    const modelContext = React.useContext(RuleEditorModelContext);
    const { registerError } = useErrorHandler();
    const [t] = useTranslation();
    const uniqueId = `${nodeId}_${ruleParameter.parameterId}`;
    const defaultValueWithLabel = ruleParameter.currentValue() ?? ruleParameter.initialValue;
    const defaultValue = ruleEditorNodeParameterValue(defaultValueWithLabel);
    const _inputAttributes = {
        id: uniqueId,
        name: uniqueId,
        onChange,
        intent: hasValidationError ? Intent.DANGER : undefined,
        readOnly: ruleEditorContext.readOnlyMode || modelContext.readOnly,
    };
    const stringDefaultValue =
        typeof defaultValue === "string" ? defaultValue : defaultValue != null ? `${defaultValue}` : undefined;
    const stringValueInputAttributes = {
        ..._inputAttributes,
        defaultValue: stringDefaultValue,
    };
    const booleanDefaultValue =
        typeof defaultValue === "boolean"
            ? defaultValue
            : typeof defaultValue === "string"
            ? defaultValue.toLowerCase() === "true"
            : undefined;
    const booleanValueInputAttributes = {
        ..._inputAttributes,
        defaultValue: booleanDefaultValue ? "true" : "false",
        defaultChecked: booleanDefaultValue,
    };

    const handleFileSearch = async (input: string) => {
        try {
            return (
                await requestResourcesList(ruleEditorContext.projectId, {
                    searchText: input,
                })
            ).data;
        } catch (e) {
            registerError("RuleParameterInput.handleFileSearch", "Could not fetch project resource files!", e);
            return [];
        }
    };

    switch (ruleParameter.parameterSpecification.type) {
        case "textArea":
            if (large) {
                // FIXME: CodeEditor looks buggy in the modal
                // return <CodeEditor {...inputAttributes} />;
                return (
                    <TextAreaWithCharacterWarnings {...stringValueInputAttributes} fill={true} large={true} rows={10} />
                );
            } else {
                return (
                    <TextAreaWithCharacterWarnings
                        {...stringValueInputAttributes}
                        fill={false}
                        small={true}
                        growVertically={false}
                        rows={2}
                        {...preventEventsFromBubblingToReactFlow}
                    />
                );
            }
        case "boolean":
            return (
                <Switch
                    {...booleanValueInputAttributes}
                    onChange={(value: boolean) => booleanValueInputAttributes.onChange(`${value}`)}
                    disabled={booleanValueInputAttributes.readOnly}
                />
            );
        case "code":
            // FIXME: Add readOnly mode
            return <CodeEditor {...stringValueInputAttributes} />;
        case "password":
            return (
                <TextField
                    {...stringValueInputAttributes}
                    type={"password"}
                    onChange={(e: React.ChangeEvent<HTMLInputElement>) => {
                        onChange(e.target.value);
                    }}
                    {...preventEventsFromBubblingToReactFlow}
                />
            );
        case "resource":
            const resourceNameFn = (item: IProjectResource) => fileValue(item);
            return (
                <SelectFileFromExisting
                    autocomplete={{
                        onSearch: handleFileSearch,
                        itemRenderer: resourceNameFn,
                        itemValueRenderer: resourceNameFn,
                        itemValueSelector: resourceNameFn,
                        itemValueString: resourceNameFn,
                        noResultText: t("common.messages.noResults", "No results."),
                        inputProps: {
                            readOnly: stringValueInputAttributes.readOnly,
                        },
                    }}
                    required={ruleParameter.parameterSpecification.required}
                    {...stringValueInputAttributes}
                    insideModal={insideModal}
                />
            );
        case "int":
        case "float":
        case "textField":
        default:
            if (ruleParameter.parameterSpecification.autoCompletion) {
                return (
                    <ParameterAutoCompletion
                        projectId={ruleEditorContext.projectId}
                        paramId={ruleParameter.parameterId}
                        pluginId={pluginId}
                        onChange={stringValueInputAttributes.onChange}
                        initialValue={
                            stringDefaultValue
                                ? {
                                      value: stringDefaultValue,
                                      label: (defaultValueWithLabel as IOperatorNodeParameterValueWithLabel)?.label,
                                  }
                                : undefined
                        }
                        autoCompletion={ruleParameter.parameterSpecification.autoCompletion}
                        intent={hasValidationError ? Intent.DANGER : Intent.NONE}
                        formParamId={uniqueId}
                        dependentValue={dependentValue}
                        required={ruleParameter.parameterSpecification.required}
                        readOnly={stringValueInputAttributes.readOnly}
                        hasBackDrop={!insideModal}
                    />
                );
            } else {
                return (
                    <TextFieldWithCharacterWarnings
                        {...stringValueInputAttributes}
                        {...preventEventsFromBubblingToReactFlow}
                    />
                );
            }
    }
};

const preventEventsFromBubblingToReactFlow = {
    onMouseDown: (event: MouseEvent<any>) => event.stopPropagation(),
    onMouseUp: (event: MouseEvent<any>) => event.stopPropagation(),
    onContextMenu: (event: MouseEvent<any>) => event.stopPropagation(),
};
