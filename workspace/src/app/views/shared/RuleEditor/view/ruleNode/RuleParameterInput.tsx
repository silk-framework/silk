import { IRuleNodeParameter } from "./RuleNodeParameter.typings";
import React from "react";
import { Switch, TextArea, TextField } from "gui-elements";
import { CodeEditor } from "../../../QueryEditor/CodeEditor";
import { FileSelectionMenu } from "../../../FileUploader/FileSelectionMenu";
import { requestResourcesList } from "@ducks/shared/requests";
import { AppToaster } from "../../../../../services/toaster";
import { Intent } from "@blueprintjs/core";
import { useTranslation } from "react-i18next";
import { useSelector } from "react-redux";
import { commonSel } from "@ducks/common";
import { RuleEditorContext } from "../../contexts/RuleEditorContext";
import { ruleEditorNodeParameterValue } from "../../model/RuleEditorModel.typings";
import { RuleEditorModelContext } from "../../contexts/RuleEditorModelContext";

interface RuleParameterInputProps {
    ruleParameter: IRuleNodeParameter;
    /** The ID of the node the parameter is part of. */
    nodeId: string;
    /** If there should be error highlighting of the input component. */
    hasValidationError: boolean;
}

/** An input widget for a parameter value. */
export const RuleParameterInput = ({ ruleParameter, nodeId, hasValidationError }: RuleParameterInputProps) => {
    const onChange = ruleParameter.update;
    const ruleEditorContext = React.useContext(RuleEditorContext);
    const modelContext = React.useContext(RuleEditorModelContext);
    const { maxFileUploadSize } = useSelector(commonSel.initialSettingsSelector);
    const [t] = useTranslation();
    const uniqueId = `${nodeId} ${ruleParameter.parameterId}`;
    const inputAttributes = {
        id: uniqueId,
        name: uniqueId,
        defaultValue: ruleEditorNodeParameterValue(ruleParameter.currentValue() ?? ruleParameter.initialValue),
        onChange,
        intent: hasValidationError ? Intent.DANGER : undefined,
        disabled: ruleEditorContext.readOnlyMode || modelContext.readOnly,
    };
    switch (ruleParameter.parameterSpecification.type) {
        case "textArea":
            return (
                <TextArea
                    {...inputAttributes}
                    onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) => {
                        onChange(e.target.value);
                    }}
                />
            );
        case "boolean":
            return <Switch {...inputAttributes} />;
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
                />
            );
        case "resource":
            const resourceNameFn = (item) => item.name;
            const handleFileSearch = async (input: string) => {
                try {
                    return (
                        await requestResourcesList(ruleEditorContext.projectId, {
                            searchText: input,
                        })
                    ).data;
                } catch (e) {
                    AppToaster.show({
                        message: e.detail,
                        intent: Intent.DANGER,
                        timeout: 0,
                    });
                    return [];
                }
            };
            return (
                <FileSelectionMenu
                    projectId={ruleEditorContext.projectId}
                    advanced={{
                        autocomplete: {
                            onSearch: handleFileSearch,
                            itemRenderer: resourceNameFn,
                            itemValueRenderer: resourceNameFn,
                            itemValueSelector: resourceNameFn,
                            noResultText: t("common.messages.noResults", "No results."),
                        },
                    }}
                    allowMultiple={false}
                    maxFileUploadSizeBytes={maxFileUploadSize}
                    onUploadSuccess={(file) => {
                        if (file) {
                            onChange(file.name);
                        }
                    }}
                    {...inputAttributes}
                />
            );
        case "int":
        case "float":
        case "textField":
        default:
            return (
                <TextField
                    {...inputAttributes}
                    onChange={(e: React.ChangeEvent<HTMLInputElement>) => {
                        onChange(e.target.value);
                    }}
                />
            );
    }
};
