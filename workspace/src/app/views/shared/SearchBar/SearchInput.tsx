import React, { memo } from "react";
import { useTranslation } from "react-i18next";
import { SearchField } from "@eccenca/gui-elements";
import { InvisibleCharacterWarningProps } from "@eccenca/gui-elements/src/components/TextField/useTextValidation";

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
    /**
     * If set, allows to be informed of invisible, hard to spot characters in the string value.
     */
    invisibleCharacterWarning?: InvisibleCharacterWarningProps;
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
    const inputRef = React.useRef<HTMLInputElement | null>(null);

    const handleKeyDown = (e) => {
        if (e.key === "Enter") {
            onEnter();
        }

        if (e.key === "Escape") {
            if (filterValue?.length) {
                onClearanceHandler();
            } else {
                inputRef.current?.blur();
            }
        }
    };

    return (
        <SearchField
            {...restProps}
            inputRef={inputRef}
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
