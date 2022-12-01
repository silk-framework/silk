import { SearchField } from "@eccenca/gui-elements";
import React, { memo } from "react";
import { useTranslation } from "react-i18next";

export interface ISearchInputProps {
    onFilterChange(e);
    onBlur?();
    onEnter();
    filterValue?: string;
    onClearanceHandler?();
    // The message that is shown when the search input is empty
    emptySearchInputMessage?: string;
    // Gives the search input the focus if true
    focusOnCreation?: boolean;
}

const SearchInput = ({
    onFilterChange,
    filterValue,
    onEnter,
    onBlur = () => {},
    onClearanceHandler = () => {},
    emptySearchInputMessage = "Enter search term",
    focusOnCreation = false,
    ...restProps
}: ISearchInputProps) => {
    const [t] = useTranslation();
    const handleKeyDown = (e) => {
        if (e.key === "Enter") {
            onEnter();
        }
    };

    return (
        <SearchField
            {...restProps}
            autoFocus={focusOnCreation}
            onChange={onFilterChange}
            onBlur={onBlur}
            onKeyDown={handleKeyDown}
            value={filterValue}
            onClearanceHandler={onClearanceHandler}
            onClearanceText={t("common.action.clearInput", "Clear input")}
            emptySearchInputMessage={emptySearchInputMessage}
        />
    );
};

const areEqual = (p: ISearchInputProps, n: ISearchInputProps) =>
    p.filterValue === n.filterValue && p.emptySearchInputMessage === n.emptySearchInputMessage;
export default memo<ISearchInputProps>(SearchInput, areEqual);
