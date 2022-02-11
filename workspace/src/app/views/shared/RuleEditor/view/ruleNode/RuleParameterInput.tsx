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

interface RuleParameterInputProps extends IRuleNodeParameter {
    /** The ID of the node. */
    nodeId: string;
}

/** An input widget for a parameter value. */
export const RuleParameterInput = (ruleParameter: RuleParameterInputProps) => {
    const onChange = ruleParameter.update;
    const ruleEditorContext = React.useContext(RuleEditorContext);
    const { maxFileUploadSize } = useSelector(commonSel.initialSettingsSelector);
    const [t] = useTranslation();
    const uniqueId = `${ruleParameter.nodeId} ${ruleParameter.parameterId}`;
    const inputAttributes = {
        id: uniqueId,
        name: uniqueId,
        defaultValue: ruleParameter.currentValue() ?? ruleParameter.initialValue,
        onChange,
    };
    switch (ruleParameter.parameterSpecification.type) {
        case "textArea":
            return <TextArea {...inputAttributes} />;
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
