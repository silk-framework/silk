import {
    ParameterAutoCompletion,
    ParameterAutoCompletionProps,
} from "../../../modals/CreateArtefactModal/ArtefactForms/ParameterAutoCompletion";
import React from "react";
import { IAutocompleteDefaultResponse } from "@ducks/shared/typings";
import { Button } from "@eccenca/gui-elements";

interface Props {
    parameterAutoCompletionProps: ParameterAutoCompletionProps;
    languageFilterSupport?: boolean;
}

/** Extracts the language part from a language filter operator string. */
const languageFilterRegex = /\[@lang\s*=\s*'([a-zA-Z-]+)']$/;

export const PathInputOperator = ({ parameterAutoCompletionProps, languageFilterSupport = true }: Props) => {
    const [activeProps, setActiveProps] = React.useState<ParameterAutoCompletionProps>(parameterAutoCompletionProps);
    const [languageFilter, setLanguageFilter] = React.useState<string | undefined>(undefined);
    const activeOnChangeHandler = React.useRef<(value: IAutocompleteDefaultResponse) => any>();
    const activeInitialValue = React.useRef<string | undefined>();
    // This onChange handler uses the up-to-date language filter
    activeOnChangeHandler.current = (value) => {
        const fullValue = {
            ...value,
            value: value.value + languageFilterExpression(languageFilter),
        };
        return parameterAutoCompletionProps.onChange(fullValue);
    };

    const overwrittenProps: Partial<ParameterAutoCompletionProps> = languageFilterSupport
        ? {
              inputProps: {
                  rightElement: <Button>{languageFilter ?? "Select"}</Button>,
              },
          }
        : {};

    if (languageFilterSupport && activeInitialValue.current == null) {
        const initialValue = parameterAutoCompletionProps.initialValue?.value ?? "";
        const match = languageFilterRegex.exec(initialValue);
        if (match) {
            const lang = match[1];
            const pathWithoutLangFilter = initialValue.substring(0, match.index);
            const onChange = (value: IAutocompleteDefaultResponse) => {
                return activeOnChangeHandler.current!(value);
            };
            const newInitialValue = {
                value: pathWithoutLangFilter,
                label: parameterAutoCompletionProps.initialValue!.label,
            };
            const newProps: ParameterAutoCompletionProps = {
                ...parameterAutoCompletionProps,
                initialValue: newInitialValue,
                onChange,
            };
            setActiveProps(newProps);
            setLanguageFilter(lang);
            overwrittenProps.initialValue = newInitialValue;
            activeInitialValue.current = pathWithoutLangFilter;
        }
    }

    return <ParameterAutoCompletion {...activeProps} {...overwrittenProps} />;
};

const languageFilterExpression = (lang: string | undefined) => {
    return lang ? `[@lang = '${lang}']` : "";
};
