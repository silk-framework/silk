import { Icon, SearchField } from "gui-elements";
import React from "react";
import { useTranslation } from "react-i18next";

interface SidebarSearchFieldProps {
    onQueryChange: (textQuery) => any;
}

/** The search input of the rule editor sidebar. */
export const SidebarSearchField = ({ onQueryChange }: SidebarSearchFieldProps) => {
    const [textQuery, setTextQuery] = React.useState<string>("");
    const [t] = useTranslation();

    // Reset the text search
    const clearSearchTerm = () => {
        setTextQuery("");
    };

    React.useEffect(() => {
        const timeout: number = window.setTimeout(() => {
            onQueryChange(textQuery.toLowerCase());
        }, 200);
        return () => {
            clearTimeout(timeout);
        };
    }, [textQuery]);

    return (
        <SearchField
            data-test-id={"rule-editor-operator-search"}
            onChange={(e) => setTextQuery(e.target.value)}
            value={textQuery}
            fullWidth={true}
            placeholder={t("RuleEditor.sidebar.searchInput")}
            onClearanceHandler={clearSearchTerm}
            leftIcon={
                <Icon
                    name="operation-search"
                    tooltipOpenDelay={500}
                    tooltipText={t("RuleEditor.sidebar.searchTooltip")}
                />
            }
        />
    );
};
