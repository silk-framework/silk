import { Icon, SearchField, AutoSuggestionList } from "@eccenca/gui-elements";
import { CodeAutocompleteFieldSuggestionWithReplacementInfo } from "@eccenca/gui-elements/src/components/AutoSuggestion/AutoSuggestion";
import React from "react";
import { useTranslation } from "react-i18next";

interface SidebarSearchFieldProps {
    activeTabId?: string;
    onQueryChange: (textQuery) => any;
    searchSuggestions?: () => CodeAutocompleteFieldSuggestionWithReplacementInfo[];
}

/** The search input of the rule editor sidebar. */
export const SidebarSearchField = ({ onQueryChange, searchSuggestions, activeTabId }: SidebarSearchFieldProps) => {
    const [textQuery, setTextQuery] = React.useState<string>("");
    const [suggestions, setSuggestions] = React.useState<CodeAutocompleteFieldSuggestionWithReplacementInfo[]>([]);
    const [hasFocus, setHasFocus] = React.useState(false);
    const [t] = useTranslation();
    const inputRef = React.useRef<HTMLInputElement>(null);

    React.useEffect(() => {
        // Select search text on tab change
        const input = inputRef.current;
        if (input && textQuery) {
            input.select();
            input.focus();
        }
    }, [activeTabId]);

    React.useEffect(() => {
        const timeout: number = window.setTimeout(() => {
            onQueryChange(textQuery.toLowerCase());
        }, 200);
        return () => {
            clearTimeout(timeout);
        };
    }, [textQuery]);

    React.useEffect(() => {
        if (textQuery.trim() === "" && searchSuggestions) {
            setSuggestions(searchSuggestions());
        } else {
            setSuggestions([]);
        }
    }, [textQuery]);

    const clearSearchTerm = React.useCallback(() => {
        setTextQuery("");
    }, []);

    return (
        <>
            <SearchField
                key={"search"}
                inputRef={inputRef}
                data-test-id={"rule-editor-operator-search"}
                onChange={(e) => setTextQuery(e.target.value)}
                value={textQuery}
                emptySearchInputMessage={t("RuleEditor.sidebar.searchInput")}
                onClearanceHandler={clearSearchTerm}
                onFocus={() => setHasFocus(true)}
                onBlur={() => setHasFocus(false)}
                leftIcon={
                    <Icon
                        name="operation-search"
                        tooltipText={t("RuleEditor.sidebar.searchTooltip")}
                        tooltipProps={{
                            hoverOpenDelay: 500,
                        }}
                    />
                }
            />
            <AutoSuggestionList
                itemToHighlight={(item) => {}}
                currentlyFocusedIndex={0}
                options={suggestions}
                isOpen={hasFocus && suggestions.length > 0}
                onItemSelectionChange={(item) => {
                    setTextQuery(item.value + " ");
                }}
                style={{ top: "auto" }}
            />
        </>
    );
    // FIXME: Too many issues with AutoSuggestion, see FIXMEs below
    // <AutoSuggestion
    //     id={"rule-editor-operator-search"}
    //     fetchSuggestions={fetchSuggestions}
    //     initialValue={""}
    //     onChange={value => setTextQuery(value)}
    //     // FIXME: Placeholder is too big and dark and stretches the input field beyond the sidebar
    //     // placeholder={t("RuleEditor.sidebar.searchInput")}
    //     // FIXME: Instead of the scroll bar the input increases in size
    //     // showScrollBar={false}
    //     leftElement={
    //         <Icon
    //             name="operation-search"
    //             tooltipOpenDelay={500}
    //             tooltipText={t("RuleEditor.sidebar.searchTooltip")}
    //         />
    //     }
    // />
};
