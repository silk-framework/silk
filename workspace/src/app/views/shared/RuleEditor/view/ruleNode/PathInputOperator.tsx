import {
    ParameterAutoCompletion,
    ParameterAutoCompletionProps,
} from "../../../modals/CreateArtefactModal/ArtefactForms/ParameterAutoCompletion";
import React from "react";
import { IAutocompleteDefaultResponse } from "@ducks/shared/typings";
import { Button, CodeAutocompleteField, IconButton, MenuItem, Select } from "@eccenca/gui-elements";
import { useTranslation } from "react-i18next";
import { checkValuePathValidity } from "../../../../../views/pages/MappingEditor/HierarchicalMapping/store";

/** Language filter related properties. */
export interface LanguageFilterProps {
    enabled: boolean;
    /** Returns for a path the type of the given path. Returns undefined if the path metadata is unknown. */
    pathType: (path: string) => string | undefined;
}

interface Props {
    parameterAutoCompletionProps: ParameterAutoCompletionProps;
    languageFilterSupport?: LanguageFilterProps;
}

/** Extracts the language part from a language filter operator string. */
const languageFilterRegex = /\[@lang\s*=\s*'([a-zA-Z0-9-]+)']$/;

const languageTagRegex = /^[a-zA-Z]+(?:-[a-zA-Z0-9]+)*$/;
const NO_LANG = "-";

const DEFAULT_LANGUAGE_FILTER_SUPPORT = { enabled: true, pathType: () => undefined };

/** Rule operator that takes input paths. */
export const PathInputOperator = ({
    parameterAutoCompletionProps,
    languageFilterSupport = DEFAULT_LANGUAGE_FILTER_SUPPORT,
}: Props) => {
    const [t] = useTranslation();
    const [activeProps, setActiveProps] = React.useState<ParameterAutoCompletionProps>(parameterAutoCompletionProps);
    const [initialLanguageFilter, setInitialLanguageFilter] = React.useState<string | undefined>()
    const internalState = React.useRef<{
        initialized: boolean;
        // The current value without the added language filter
        currentValue?: IAutocompleteDefaultResponse;
        // The current filter language
        currentLanguageFilter?: string;
        // This onChange handler uses the up-to-date language filter
        activeOnChangeHandler?: (value: IAutocompleteDefaultResponse) => any;
    }>({ initialized: false, currentValue: parameterAutoCompletionProps.initialValue });
    const [showLanguageFilterButton, setShowLanguageFilterButton] = React.useState(false);

    const checkPathToShowFilterButton = React.useCallback((path?: string) => {
        const pathType = path ? languageFilterSupport.pathType(path) : "URI";
        setShowLanguageFilterButton(!pathType || pathType === "String");
    }, []);

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
        internalState.current.activeOnChangeHandler!(
            internalState.current.currentValue ?? { value: "" }
        );
        internalState.current.currentLanguageFilter = langValue;
    }, [])

    internalState.current.activeOnChangeHandler = (value) => {
        internalState.current.currentValue = value;
        checkPathToShowFilterButton(value.value);
        const fullValue = {
            ...value,
            value: value.value + languageFilterExpression(internalState.current.currentLanguageFilter),
        };
        return parameterAutoCompletionProps.onChange(fullValue);
    };

    const overwrittenProps: Partial<ParameterAutoCompletionProps> = React.useMemo(() =>
            languageFilterSupport.enabled
                ? {
                    inputProps: {
                        rightElement: <LanguageSwitcher
                            onLanguageChange={onLanguageChange}
                            initialLanguage={initialLanguageFilter}
                        />,
                    },
                }
                : {},
        [languageFilterSupport.enabled, onLanguageChange, initialLanguageFilter])

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
    const onChange = React.useMemo(() => (value: string) => activeProps.onChange({ value }), [activeProps.onChange])
    const fetchSuggestion = React.useMemo(() => {
        const inputType = activeProps.pluginId.replace("PathInput", "") as "source" | "target";
        return (activeProps.partialAutoCompletion && activeProps.partialAutoCompletion(inputType)) ||
            (async () => undefined)
    }, [activeProps.partialAutoCompletion, activeProps.pluginId]);
    const checkInput = React.useMemo(() => {
        return (value) => checkValuePathValidity(value, activeProps.projectId)
    }, [activeProps.projectId])

    const autoCompletionInput = React.useMemo(() => {
        return (
            <CodeAutocompleteField
                {...overwrittenProps.inputProps}
                initialValue={initialValue}
                onChange={onChange}
                fetchSuggestions={fetchSuggestion}
                placeholder={t("ActiveLearning.config.manualSelection.insertPath")}
                checkInput={checkInput}
                validationErrorText={t("ActiveLearning.config.errors.invalidPath")}
                autoCompletionRequestDelay={500}
                validationRequestDelay={250}
            />
        );
    }, [fetchSuggestion, initialValue, onChange, checkInput, overwrittenProps]);

    return <LanguageSwitcherContext.Provider value={{
        readOnly: parameterAutoCompletionProps.readOnly,
        showLanguageFilterButton
    }}>
        {autoCompletionInput}
    </LanguageSwitcherContext.Provider>

    // return <ParameterAutoCompletion {...activeProps} {...overwrittenProps} showErrorsInline={true} />;
};

const languageFilterExpression = (lang: string | undefined) => {
    return lang ? `[@lang = '${lang}']` : "";
};

const languageFilterItems = ["en", "de", "fr", NO_LANG];

interface LanguageSwitcherProps {
    onLanguageChange: (lang: string | undefined) => void
    initialLanguage: string | undefined
}

interface LanguageSwitcherContextProps {
    showLanguageFilterButton: boolean
    readOnly?: boolean
}

const LanguageSwitcherContext = React.createContext<LanguageSwitcherContextProps>({
    showLanguageFilterButton: false,
})

const LanguageSwitcher = ({onLanguageChange, initialLanguage}: LanguageSwitcherProps) => {
    const [t] = useTranslation();
    const [languageFilter, setLanguageFilter] = React.useState<string | undefined>(initialLanguage);
    const currentLanguageFilter = React.useRef<string | undefined>()
    currentLanguageFilter.current = languageFilter
    const context = React.useContext(LanguageSwitcherContext)

    return <Select<string>
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
            handleClick: React.MouseEventHandler<HTMLElement>
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
            onLanguageChange(langValue)
            setLanguageFilter(langValue)
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
}
