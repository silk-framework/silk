import React, { useEffect } from "react";
import { INPUT_TYPES } from "../../../../../constants";
import { CodeEditor, Spinner, Switch, TextField } from "@eccenca/gui-elements";
import { ITaskParameter } from "@ducks/common/typings";
import { IntentTypes as Intent } from "@eccenca/gui-elements/src/common/Intent";
import { FileSelectionMenu } from "../../../FileUploader/FileSelectionMenu";
import { requestResourcesList } from "@ducks/shared/requests";
import { defaultValueAsJs, stringValueAsJs } from "../../../../../utils/transformers";
import { useSelector } from "react-redux";
import { commonSel } from "@ducks/common";
import { useTranslation } from "react-i18next";
import { DefaultTargetVocabularySelection } from "../../../TargetVocabularySelection/DefaultTargetVocabularySelection";
import { fileValue } from "@ducks/shared/typings";
import { ExtendedParameterCallbacks } from "./ParameterWidget";
import { TextFieldWithCharacterWarnings } from "../../../extendedGuiElements/TextFieldWithCharacterWarnings";
import { TextAreaWithCharacterWarnings } from "../../../extendedGuiElements/TextAreaWithCharacterWarnings";
import useErrorHandler from "../../../../../hooks/useErrorHandler";
import { CreateArtefactModalContext } from "../CreateArtefactModalContext";

interface IProps {
    projectId: string;
    parameter: ITaskParameter;
    // Blueprint intent
    intent: Intent;
    onChange: (value) => void;
    // Initial values in a flat form, e.g. "nestedParam.param1". This is either set for all parameters or not set for none.
    // The prefixed values can be addressed with help of the 'formParamId' parameter.
    initialParameterValue?: string;
    /** This is a required parameter. */
    required: boolean;
    parameterCallbacks: ExtendedParameterCallbacks;
}

export type RegisterForExternalChangesFn = (
    paramId: string,
    handleUpdates: (value: { value: string; label?: string }) => any,
) => any;

/** The attributes for the GUI components. */
export interface IInputAttributes {
    id: string;
    name: string;
    intent: Intent;
    onChange: (value) => void;
    value?: any;
    defaultValue?: any;
    inputRef?: (e) => void;
    defaultChecked?: boolean;
}

/** Maps an atomic value to the corresponding value type widget. */
export function InputMapper({
    projectId,
    parameter,
    intent,
    onChange,
    initialParameterValue,
    required,
    parameterCallbacks,
}: IProps) {
    const [t] = useTranslation();
    const { registerError: globalErrorHandler } = useErrorHandler();
    const modalContext = React.useContext(CreateArtefactModalContext);
    const registerError = modalContext.registerModalError ? modalContext.registerModalError : globalErrorHandler;
    const { maxFileUploadSize } = useSelector(commonSel.initialSettingsSelector);
    const { paramId, param } = parameter;
    const [externalValue, setExternalValue] = React.useState<{ value: string; label?: string } | undefined>(undefined);
    const [show, setShow] = React.useState(true);
    const [highlightInput, setHighlightInput] = React.useState(false);
    const initialOrExternalValue = externalValue ? externalValue.value : initialParameterValue;
    const initialValue =
        initialOrExternalValue != null
            ? stringValueAsJs(parameter.param.parameterType, initialOrExternalValue)
            : defaultValueAsJs(param, false);

    let onChangeUsed = onChange;
    if (highlightInput) {
        onChangeUsed = (value: any) => {
            onChange(value);
            setHighlightInput(false);
        };
    }

    useEffect(() => {
        const handleUpdates = (externalValue: { value: string; label?: string }) => {
            setExternalValue(externalValue);
            setHighlightInput(true);
            onChange(stringValueAsJs(parameter.param.parameterType, externalValue.value));
        };
        parameterCallbacks.registerForExternalChanges(paramId, handleUpdates);
    }, []);

    // Re-init element when value is set from outside
    useEffect(() => {
        if (externalValue) {
            setShow(false);
            setTimeout(() => setShow(true), 0);
        }
    }, [externalValue]);

    const inputAttributes: IInputAttributes = {
        id: paramId,
        name: paramId,
        intent: highlightInput ? "success" : intent,
        onChange: onChangeUsed,
        defaultValue: initialValue,
    };

    const handleFileSearch = async (input: string) => {
        try {
            return (
                await requestResourcesList(projectId, {
                    searchText: input,
                })
            ).data;
        } catch (e) {
            registerError("InputMapper.handleFileSearch", "File search has failed.", e);
            return [];
        }
    };

    if (param.parameterType === INPUT_TYPES.BOOLEAN) {
        inputAttributes.defaultChecked = initialValue;
    }

    if (!show) {
        return <Spinner />;
    }

    if (param.parameterType.startsWith("code-")) {
        const mode = param.parameterType.substring(5);
        if (CodeEditor.supportedModes.find((m) => m === mode)) {
            return <CodeEditor mode={mode as any} {...inputAttributes} />;
        } else {
            return <CodeEditor {...inputAttributes} />;
        }
    }

    switch (param.parameterType) {
        case INPUT_TYPES.BOOLEAN:
            return <Switch {...inputAttributes} />;
        // NumericInput does not support onChange, see https://github.com/palantir/blueprint/issues/3943
        case INPUT_TYPES.INTEGER:
            return <TextField {...inputAttributes} escapeToBlur={true} />;
        case INPUT_TYPES.TEXTAREA:
            return <TextAreaWithCharacterWarnings {...inputAttributes} />;
        case INPUT_TYPES.RESTRICTION:
            return <CodeEditor mode="sparql" {...inputAttributes} />;
        case INPUT_TYPES.MULTILINE_STRING:
            return <CodeEditor {...inputAttributes} />;
        case INPUT_TYPES.PASSWORD:
            return <TextField {...inputAttributes} type={"password"} escapeToBlur={true} />;
        case INPUT_TYPES.TARGET_VOCABULARY:
            return <DefaultTargetVocabularySelection {...inputAttributes} />;
        case INPUT_TYPES.RESOURCE:
            const resourceNameFn = (item) => fileValue(item);
            return (
                <FileSelectionMenu
                    projectId={projectId}
                    t={t}
                    advanced={{
                        autocomplete: {
                            onSearch: handleFileSearch,
                            itemRenderer: resourceNameFn,
                            itemValueRenderer: resourceNameFn,
                            itemValueSelector: resourceNameFn,
                            noResultText: t("common.messages.noResults", "No results."),
                            itemValueString: () => "",
                        },
                    }}
                    allowMultiple={false}
                    maxFileUploadSizeBytes={maxFileUploadSize}
                    onUploadSuccess={(file) => {
                        // FIXME: the onChange function is not called on upload success, so this is a workaround
                        if (file) {
                            onChange(file.name);
                        }
                    }}
                    required={required}
                    {...inputAttributes}
                />
            );
        case INPUT_TYPES.ENUMERATION:
        case INPUT_TYPES.OPTION_INT:
        case INPUT_TYPES.STRING:
        default:
            return <TextFieldWithCharacterWarnings {...inputAttributes} escapeToBlur={true} />;
    }
}
