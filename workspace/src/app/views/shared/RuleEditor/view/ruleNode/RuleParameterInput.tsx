import { IRuleNodeParameter } from "./RuleNodeParameter.typings";
import React, { MouseEvent } from "react";
import { Switch, TextArea, TextField } from "@eccenca/gui-elements";
import { CodeEditor } from "../../../QueryEditor/CodeEditor";
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
import { IProjectResource } from "@ducks/shared/typings";

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
}

/** An input widget for a parameter value. */
export const RuleParameterInput = ({
    pluginId,
    ruleParameter,
    nodeId,
    hasValidationError,
    dependentValue,
    large,
}: RuleParameterInputProps) => {
    const onChange = ruleParameter.update;
    const ruleEditorContext = React.useContext(RuleEditorContext);
    const modelContext = React.useContext(RuleEditorModelContext);
    const { registerError } = useErrorHandler();
    const [t] = useTranslation();
    const uniqueId = `${nodeId}_${ruleParameter.parameterId}`;
    const defaultValueWithLabel = ruleParameter.currentValue() ?? ruleParameter.initialValue;
    const defaultValue = ruleEditorNodeParameterValue(defaultValueWithLabel);
    const inputAttributes = {
        id: uniqueId,
        name: uniqueId,
        defaultValue: defaultValue,
        onChange,
        intent: hasValidationError ? Intent.DANGER : undefined,
        disabled: ruleEditorContext.readOnlyMode || modelContext.readOnly,
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
                    <TextArea
                        {...inputAttributes}
                        fill={true}
                        large={true}
                        onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) => {
                            onChange(e.target.value);
                        }}
                        rows={10}
                    />
                );
            } else {
                return (
                    <TextArea
                        {...inputAttributes}
                        fill={false}
                        small={true}
                        growVertically={false}
                        onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) => {
                            onChange(e.target.value);
                        }}
                        rows={2}
                        {...preventEventsFromBubblingToReactFlow}
                    />
                );
            }
        case "boolean":
            return (
                <Switch
                    {...inputAttributes}
                    onChange={(value: boolean) => inputAttributes.onChange(`${value}`)}
                    defaultChecked={inputAttributes.defaultValue?.toLowerCase() === "true"}
                />
            );
        case "code":
            return <CodeEditor {...inputAttributes} />;
        case "password":
            return (
                <TextField
                    {...inputAttributes}
                    type={"password"}
                    onChange={(e: React.ChangeEvent<HTMLInputElement>) => {
                        onChange(e.target.value);
                    }}
                    {...preventEventsFromBubblingToReactFlow}
                />
            );
        case "resource":
            const resourceNameFn = (item: IProjectResource) => item.name;
            return (
                <SelectFileFromExisting
                    autocomplete={{
                        onSearch: handleFileSearch,
                        itemRenderer: resourceNameFn,
                        itemValueRenderer: resourceNameFn,
                        itemValueSelector: resourceNameFn,
                        itemValueString: resourceNameFn,
                        noResultText: t("common.messages.noResults", "No results."),
                    }}
                    {...inputAttributes}
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
                        onChange={inputAttributes.onChange}
                        initialValue={
                            defaultValue
                                ? {
                                      value: defaultValue,
                                      label: (defaultValueWithLabel as IOperatorNodeParameterValueWithLabel)?.label,
                                  }
                                : undefined
                        }
                        autoCompletion={ruleParameter.parameterSpecification.autoCompletion}
                        intent={hasValidationError ? Intent.DANGER : Intent.NONE}
                        formParamId={uniqueId}
                        dependentValue={dependentValue}
                        required={ruleParameter.parameterSpecification.required}
                    />
                );
            } else {
                return (
                    <TextField
                        {...inputAttributes}
                        onChange={(e: React.ChangeEvent<HTMLInputElement>) => {
                            onChange(e.target.value);
                        }}
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
