import {
    SuggestField,
    Button,
    Highlighter,
    highlighterUtils,
    MenuItem,
    OverflowText,
    SimpleDialog,
} from "@eccenca/gui-elements";
import React from "react";
import { IUriPattern } from "../../../../api/types";

interface IProps {
    // The selection of URI patterns
    uriPatterns: IUriPattern[];
    // Called when a specific URI pattern has been selected
    onSelect: (uriPattern: IUriPattern) => any;
    // Called when pressing the close button, but also after selecting an entry.
    onClose: () => any;
}

/** Allows to select a URI pattern from a list of candidates. */
export const UriPatternSelectionModal = ({ uriPatterns, onSelect, onClose }: IProps) => {
    // Renders an item
    const itemOption = (uriPattern: IUriPattern, query: string, modifiers) => {
        return (
            <MenuItem
                key={uriPattern.value}
                onClick={() => handleSelect(uriPattern)}
                active={modifiers.active}
                popoverProps={{ fill: true }}
                text={
                    <OverflowText inline={true} title={uriPattern.value} style={{ minWidth: "35vw" }}>
                        <Highlighter label={uriPattern.label} searchValue={query} />
                    </OverflowText>
                }
            />
        );
    };
    const handleSearch = (value: string) => {
        const searchWords = highlighterUtils.extractSearchWords(value, true);
        return uriPatterns.filter((up) => highlighterUtils.matchesAllWords(up.value.toLowerCase(), searchWords));
    };
    const handleSelect = (pattern: IUriPattern) => {
        onSelect(pattern);
        onClose();
    };
    return (
        <SimpleDialog
            data-test-id={"uri-pattern-selection-modal"}
            transitionDuration={20}
            onClose={onClose}
            isOpen={true}
            title={"Choose from existing URI patterns"}
            actions={
                <Button data-test-id={"uri-pattern-selection-modal-close-btn"} onClick={onClose}>
                    Close
                </Button>
            }
        >
            <SuggestField<IUriPattern, IUriPattern>
                onSearch={handleSearch}
                itemValueSelector={(value) => value}
                itemValueString={(uriPattern) => uriPattern.value}
                itemRenderer={itemOption}
                onChange={handleSelect}
                autoFocus={true}
                inputProps={{ placeholder: "Filter..." }}
                // This is used for the key generation of the option React elements, even though this is not displayed anywhere.
                itemValueRenderer={(item) => item.value}
                noResultText={"No result"}
            />
        </SimpleDialog>
    );
};
