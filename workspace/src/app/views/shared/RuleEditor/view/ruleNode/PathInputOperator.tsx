import {
    ParameterAutoCompletion,
    ParameterAutoCompletionProps,
} from "../../../modals/CreateArtefactModal/ArtefactForms/ParameterAutoCompletion";
import React from "react";
import { IAutocompleteDefaultResponse } from "@ducks/shared/typings";
import { Button, IconButton, MenuItem, Select } from "@eccenca/gui-elements";
import { useTranslation } from "react-i18next";

interface Props {
    parameterAutoCompletionProps: ParameterAutoCompletionProps;
    languageFilterSupport?: boolean;
}

/** Extracts the language part from a language filter operator string. */
const languageFilterRegex = /\[@lang\s*=\s*'([a-zA-Z0-9-]+)']$/;

const languageTagRegex = /^[a-zA-Z]+(?:-[a-zA-Z0-9]+)*$/;
const StringSelect = Select.ofType<string>();
const NO_LANG = "-";

/** Rule operator that takes input paths. */
export const PathInputOperator = ({ parameterAutoCompletionProps, languageFilterSupport = true }: Props) => {
    const [t] = useTranslation();
    const [activeProps, setActiveProps] = React.useState<ParameterAutoCompletionProps>(parameterAutoCompletionProps);
    const [languageFilter, _setLanguageFilter] = React.useState<string | undefined>(undefined);
    const internalState = React.useRef<{
        initialized: boolean;
        // The current value without the added language filter
        currentValue?: IAutocompleteDefaultResponse;
        // The current filter language
        currentLanguageFilter?: string;
        // This onChange handler uses the up-to-date language filter
        activeOnChangeHandler?: (value: IAutocompleteDefaultResponse) => any;
    }>({ initialized: false, currentValue: parameterAutoCompletionProps.initialValue });

    const setLanguageFilter = (string) => {
        internalState.current.currentLanguageFilter = string;
        _setLanguageFilter(string);
    };

    internalState.current.activeOnChangeHandler = (value) => {
        internalState.current.currentValue = value;
        const fullValue = {
            ...value,
            value: value.value + languageFilterExpression(internalState.current.currentLanguageFilter),
        };
        return parameterAutoCompletionProps.onChange(fullValue);
    };
    const languageFilterItems = ["en", "de", "fr", NO_LANG];

    const overwrittenProps: Partial<ParameterAutoCompletionProps> = languageFilterSupport
        ? {
              inputProps: {
                  rightElement: (
                      <StringSelect
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
                                  internalState.current.currentLanguageFilter ? (
                                      <MenuItem
                                          data-test-id={"language-filter-remove"}
                                          active={modifiers.active}
                                          icon={"operation-filterRemove"}
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
                              internalState.current.currentLanguageFilter = langValue;
                              // Need to call onChange handler with changed language filter
                              internalState.current.activeOnChangeHandler!(
                                  internalState.current.currentValue ?? { value: "" }
                              );
                              setLanguageFilter(langValue);
                          }}
                          disabled={!!parameterAutoCompletionProps.readOnly}
                          fill={false}
                          popoverTargetProps={{
                              style: { display: "inline-block" },
                          }}
                          popoverProps={{
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
                      </StringSelect>
                  ),
              },
          }
        : {};

    // Initialize language filter and modify original props, e.g. onChange handler and initialValue
    if (languageFilterSupport && !internalState.current.initialized) {
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
            setLanguageFilter(lang);
            overwrittenProps.initialValue = newInitialValue;
        }
        setActiveProps(newProps);
    }

    return <ParameterAutoCompletion {...activeProps} {...overwrittenProps} />;
};

const languageFilterExpression = (lang: string | undefined) => {
    return lang ? `[@lang = '${lang}']` : "";
};
