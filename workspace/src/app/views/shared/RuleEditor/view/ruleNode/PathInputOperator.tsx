import { ParameterAutoCompletionProps } from "../../../modals/CreateArtefactModal/ArtefactForms/ParameterAutoCompletion";
import React from "react";
import { IAutocompleteDefaultResponse } from "@ducks/shared/typings";
import { Button, CodeAutocompleteField, IconButton, MenuItem, Select, Spacing, Tag } from "@eccenca/gui-elements";
import { useTranslation } from "react-i18next";
import { checkValuePathValidity } from "../../../../../views/pages/MappingEditor/HierarchicalMapping/store";
import { CodeAutocompleteFieldPartialAutoCompleteResult } from "@eccenca/gui-elements/src/components/AutoSuggestion/AutoSuggestion";
import { RuleEditorEvaluationCallbackContext } from "../../contexts/RuleEditorEvaluationContext";
import { preventMouseEventsFromBubblingToReactFlow } from "./RuleParameterInput";

/** Language filter related properties. */
export interface LanguageFilterProps {
    enabled: boolean;
    /** Returns for a path the type of the given path. Returns undefined if the path metadata is unknown. */
    pathType: (path: string) => string | undefined;
}

/** Parameter functions that are specific to input path operators. */
export interface InputPathFunctions {
    /** If for this operator there is a language filter supported. */
    languageFilter?: LanguageFilterProps;
    /** Returns the label for the path. */
    inputPathLabel?: (path: string) => string | undefined;
}

interface Props {
    parameterAutoCompletionProps: ParameterAutoCompletionProps;
    inputPathFunctions: InputPathFunctions;
}

/** Extracts the language part from a language filter operator string. */
export const languageFilterRegex = /\[@lang\s*=\s*'([a-zA-Z0-9-]+)']$/;

const languageTagRegex = /^[a-zA-Z]+(?:-[a-zA-Z0-9]+)*$/;
const NO_LANG = "-";

const DEFAULT_LANGUAGE_FILTER_SUPPORT = { enabled: true, pathType: () => undefined };

/** Rule operator that takes input paths. */
export const PathInputOperator = ({ parameterAutoCompletionProps, inputPathFunctions }: Props) => {
    const [t] = useTranslation();
    const [activeProps, setActiveProps] = React.useState<ParameterAutoCompletionProps>(parameterAutoCompletionProps);
    const [initialLanguageFilter, setInitialLanguageFilter] = React.useState<string | undefined>();
    const internalState = React.useRef<{
        initialized: boolean;
        // The current value without the added language filter
        currentValue?: IAutocompleteDefaultResponse;
        // The current filter language
        currentLanguageFilter?: string;
        // This onChange handler uses the up-to-date language filter
        activeOnChangeHandler?: (value: IAutocompleteDefaultResponse) => any;
        // Current label language
        currentPathLabelLanguage: string | undefined;
    }>({
        initialized: false,
        currentValue: parameterAutoCompletionProps.initialValue,
        currentPathLabelLanguage: undefined,
    });
    const [showLanguageFilterButton, setShowLanguageFilterButton] = React.useState(false);
    const languageFilterSupport = inputPathFunctions.languageFilter ?? DEFAULT_LANGUAGE_FILTER_SUPPORT;
    const context = React.useContext(PathInputOperatorContext);
    const evaluationCallbackContext = React.useContext(RuleEditorEvaluationCallbackContext);

    const checkPathToShowFilterButton = React.useCallback((path?: string) => {
        const pathType = path ? languageFilterSupport.pathType(path) : "URI";
        setShowLanguageFilterButton(!pathType || pathType === "String");
    }, []);

    const fetchLabel = (value: string) => {
        const property = extractProperty(value);
        return inputPathFunctions.inputPathLabel?.(property ?? value);
    };

    // Update label on the fly if user new path labels are available because of user language change
    if (context.pathLabelsAvailableForLang !== internalState.current.currentPathLabelLanguage) {
        const value = internalState.current.currentValue ?? parameterAutoCompletionProps.initialValue;
        if (value) {
            const label = fetchLabel(value.value);
            if (label) {
                internalState.current.currentValue = {
                    ...value,
                    label,
                };
            }
        }
        internalState.current.currentPathLabelLanguage = context.pathLabelsAvailableForLang;
    }

    React.useEffect(() => {
        const initialValue = parameterAutoCompletionProps.initialValue;
        if (languageFilterSupport.enabled && initialValue) {
            const initialPath = parameterAutoCompletionProps.initialValue?.value;
            checkPathToShowFilterButton(initialPath);
        }
    }, [checkPathToShowFilterButton]);

    const onLanguageChange = React.useCallback((langValue: string) => {
        internalState.current.currentLanguageFilter = langValue;
        // Need to call onChange handler with changed language filter
        internalState.current.activeOnChangeHandler!(internalState.current.currentValue ?? { value: "" });
        internalState.current.currentLanguageFilter = langValue;
    }, []);

    internalState.current.activeOnChangeHandler = (v) => {
        let value = v;
        if (!value.label && inputPathFunctions.inputPathLabel) {
            // try to get label
            const label = fetchLabel(v.value);
            value = { ...v, label };
        }
        internalState.current.currentValue = value;
        checkPathToShowFilterButton(value.value);
        const fullValue = {
            ...value,
            value: value.value + languageFilterExpression(internalState.current.currentLanguageFilter),
        };
        return parameterAutoCompletionProps.onChange(fullValue);
    };

    const overwrittenProps: Partial<ParameterAutoCompletionProps> = React.useMemo(
        () =>
            languageFilterSupport.enabled
                ? {
                      inputProps: {
                          rightElement: (
                              <LanguageSwitcher
                                  onLanguageChange={onLanguageChange}
                                  initialLanguage={initialLanguageFilter}
                              />
                          ),
                      },
                  }
                : {},
        [languageFilterSupport.enabled, onLanguageChange, initialLanguageFilter],
    );

    // Initialize language filter and modify original props, e.g. onChange handler and initialValue
    if (languageFilterSupport.enabled && !internalState.current.initialized) {
        internalState.current.initialized = true;
        const initialValue = parameterAutoCompletionProps.initialValue?.value ?? "";
        const onChange = (value: IAutocompleteDefaultResponse) => {
            return internalState.current.activeOnChangeHandler!(value);
        };
        const newProps: ParameterAutoCompletionProps = {
            ...parameterAutoCompletionProps,
            onChange,
        };
        const match = languageFilterRegex.exec(initialValue);
        if (match) {
            // Extract current language filter
            const lang = match[1];
            const pathWithoutLangFilter = initialValue.substring(0, match.index);
            const newInitialValue = {
                value: pathWithoutLangFilter,
                label: parameterAutoCompletionProps.initialValue!.label,
            };
            internalState.current.currentValue = newInitialValue;
            newProps.initialValue = newInitialValue;
            setInitialLanguageFilter(lang);
            overwrittenProps.initialValue = newInitialValue;
        }
        setActiveProps(newProps);
    }

    const initialValue = React.useMemo(() => activeProps.initialValue?.value ?? "", [activeProps.initialValue]);
    const onChange = React.useMemo(() => (value: string) => activeProps.onChange({ value }), [activeProps.onChange]);
    const fetchSuggestion = React.useMemo(() => {
        const inputType = activeProps.pluginId.replace("PathInput", "") as "source" | "target";
        const fetchFunctionToUse =
            (activeProps.partialAutoCompletion && activeProps.partialAutoCompletion(inputType)) ||
            (async () => undefined);
        // Add label resolution to the fetch function
        return async (
            inputString: string,
            cursorPosition: number,
        ): Promise<CodeAutocompleteFieldPartialAutoCompleteResult | undefined> => {
            const result = await fetchFunctionToUse(inputString, cursorPosition);
            if (result) {
                return {
                    ...result,
                    replacementResults: result.replacementResults.map((replacementResult) => {
                        return {
                            ...replacementResult,
                            replacements: replacementResult.replacements.map((replacement) => {
                                if (replacement.label) {
                                    return replacement;
                                } else {
                                    // try to add label
                                    const property = extractProperty(replacement.value);
                                    const label = inputPathFunctions.inputPathLabel?.(property ?? replacement.value);
                                    return {
                                        ...replacement,
                                        label,
                                    };
                                }
                            }),
                        };
                    }),
                };
            }
        };
    }, [activeProps.partialAutoCompletion, activeProps.pluginId]);
    const checkInput = React.useMemo(() => {
        return (value) => checkValuePathValidity(value, activeProps.projectId);
    }, [activeProps.projectId]);

    const onFocusChange = React.useCallback(
        (hasFocus: boolean) => {
            evaluationCallbackContext.enableErrorModal(!hasFocus);
        },
        [evaluationCallbackContext.enableErrorModal],
    );

    const autoCompletionInput = React.useMemo(() => {
        return (
            <CodeAutocompleteField
                id={parameterAutoCompletionProps.formParamId}
                readOnly={parameterAutoCompletionProps.readOnly}
                {...overwrittenProps.inputProps}
                initialValue={initialValue}
                onChange={onChange}
                fetchSuggestions={fetchSuggestion}
                placeholder={t("ActiveLearning.config.manualSelection.insertPath")}
                checkInput={checkInput}
                validationErrorText={t("ActiveLearning.config.errors.invalidPath")}
                autoCompletionRequestDelay={500}
                validationRequestDelay={250}
                onFocusChange={onFocusChange}
                outerDivAttributes={preventMouseEventsFromBubblingToReactFlow}
            />
        );
    }, [fetchSuggestion, initialValue, onChange, checkInput, overwrittenProps, parameterAutoCompletionProps.readOnly]);

    const currentLabel = internalState.current.currentValue?.label;
    return (
        <LanguageSwitcherContext.Provider
            value={{
                readOnly: parameterAutoCompletionProps.readOnly,
                showLanguageFilterButton,
            }}
        >
            {autoCompletionInput}
            {currentLabel ? (
                <>
                    <Spacing size={"tiny"} />
                    <Tag round={true} htmlTitle={currentLabel}>
                        {currentLabel}
                    </Tag>
                </>
            ) : null}
        </LanguageSwitcherContext.Provider>
    );

    // return <ParameterAutoCompletion {...activeProps} {...overwrittenProps} showErrorsInline={true} />;
};

const languageFilterExpression = (lang: string | undefined) => {
    return lang ? `[@lang = '${lang}']` : "";
};

const languageFilterItems = ["en", "de", "fr", NO_LANG];

interface LanguageSwitcherProps {
    onLanguageChange: (lang: string | undefined) => void;
    initialLanguage: string | undefined;
}

interface LanguageSwitcherContextProps {
    showLanguageFilterButton: boolean;
    readOnly?: boolean;
}

const LanguageSwitcherContext = React.createContext<LanguageSwitcherContextProps>({
    showLanguageFilterButton: false,
});

const LanguageSwitcher = ({ onLanguageChange, initialLanguage }: LanguageSwitcherProps) => {
    const [t] = useTranslation();
    const [languageFilter, setLanguageFilter] = React.useState<string | undefined>(initialLanguage);
    const currentLanguageFilter = React.useRef<string | undefined>();
    currentLanguageFilter.current = languageFilter;
    const context = React.useContext(LanguageSwitcherContext);

    return context.showLanguageFilterButton ? (
        <Select<string>
            inputProps={{
                id: "language-filter-input",
            }}
            items={languageFilterItems}
            filterable={true}
            itemPredicate={(query, item) => item.toLowerCase().includes(query.toLowerCase().trim())}
            createNewItemFromQuery={(query) => {
                return query;
            }}
            createNewItemRenderer={(
                query: string,
                active: boolean,
                handleClick: React.MouseEventHandler<HTMLElement>,
            ) => {
                if (languageTagRegex.test(query)) {
                    return (
                        <MenuItem
                            data-test-id={"language-filter-custom"}
                            icon={"item-add-artefact"}
                            active={active}
                            key={query}
                            onClick={handleClick}
                            text={query}
                        />
                    );
                }
            }}
            itemRenderer={(lang, { handleClick, modifiers }) => {
                return lang === NO_LANG ? (
                    currentLanguageFilter.current ? (
                        <MenuItem
                            data-test-id={"language-filter-remove"}
                            active={modifiers.active}
                            icon={"operation-filterremove"}
                            text={t("PathInputOperator.noFilter")}
                            onClick={handleClick}
                        />
                    ) : null
                ) : (
                    <MenuItem
                        data-test-id={`language-filter-${lang}`}
                        active={modifiers.active}
                        icon={"operation-filter"}
                        text={lang}
                        onClick={handleClick}
                    />
                );
            }}
            onItemSelect={(lang) => {
                const langValue = lang === "-" ? undefined : lang;
                onLanguageChange(langValue);
                setLanguageFilter(langValue);
            }}
            disabled={!!context.readOnly}
            fill={false}
            contextOverlayProps={{
                hasBackdrop: true,
            }}
        >
            {languageFilter ? (
                <Button
                    data-test-id={"language-filter-btn"}
                    tooltip={t("PathInputOperator.languageButtonTooltip")}
                    outlined={true}
                >
                    {languageFilter}
                </Button>
            ) : (
                <IconButton
                    data-test-id={"language-filter-btn"}
                    text={t("PathInputOperator.languageButtonTooltip")}
                    name={"operation-translate"}
                    outlined={true}
                />
            )}
        </Select>
    ) : null;
};

/** Extract the last property of the path that is not in a filter. */
export const extractProperty = (path: string): string | undefined => {
    // Remove last part if not a property
    let currentSubPath = path.trimEnd();
    let lastChar = currentSubPath[currentSubPath.length - 1];
    while (lastChar === "]") {
        const lastQuoteIndex = currentSubPath.lastIndexOf('"');
        let lastOpenBracketIndex = currentSubPath.lastIndexOf("[");
        if (lastQuoteIndex > lastOpenBracketIndex) {
            // Remove value
            currentSubPath = currentSubPath.substring(0, lastQuoteIndex);
            const secondToLastQuoteIndex = currentSubPath.lastIndexOf('"');
            currentSubPath = currentSubPath.substring(0, secondToLastQuoteIndex);
            lastOpenBracketIndex = currentSubPath.lastIndexOf("[");
        }
        currentSubPath = currentSubPath.substring(0, lastOpenBracketIndex).trimEnd();
        lastChar = currentSubPath[currentSubPath.length - 1];
    }
    if (currentSubPath.length) {
        if (lastChar === ">") {
            const openingUriIdx = currentSubPath.lastIndexOf("<");
            if (openingUriIdx >= 0) {
                return currentSubPath.substring(openingUriIdx);
            }
        } else {
            const propertyStart =
                Math.max(
                    currentSubPath.lastIndexOf("/"),
                    currentSubPath.lastIndexOf("\\"),
                    currentSubPath.lastIndexOf("]"),
                    -1,
                ) + 1;
            return currentSubPath.substring(propertyStart).trim();
        }
    }
};

interface PathInputOperatorContextProps {
    /** The language for which path labels are available for. */
    pathLabelsAvailableForLang: string | undefined;
}

export const PathInputOperatorContext = React.createContext<PathInputOperatorContextProps>({
    pathLabelsAvailableForLang: undefined,
});
