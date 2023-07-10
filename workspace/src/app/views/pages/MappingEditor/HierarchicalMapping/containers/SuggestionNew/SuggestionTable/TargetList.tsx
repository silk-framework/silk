import React, { useContext, useEffect, useState } from "react";
import {
    MenuItem,
    Select,
    Button,
    Highlighter,
    OverflowText,
    OverviewItem,
    OverviewItemDescription,
    OverviewItemLine,
} from "@eccenca/gui-elements";
import { ITargetWithSelected } from "../suggestion.typings";
import { SuggestionListContext } from "../SuggestionContainer";
import { extractSearchWords, matchesAllWords } from "@eccenca/gui-elements/src/components/Typography/Highlighter";

// Select<T> is a generic component to work with your data types.
// In TypeScript, you must first obtain a non-generic reference:
const TargetSelect = Select.ofType<ITargetWithSelected>();

interface IProps {
    targets: ITargetWithSelected[];

    onChange(uri: ITargetWithSelected);
}
export default function TargetList({ targets, onChange }: IProps) {
    const context = useContext(SuggestionListContext);

    const [items, setItems] = useState<ITargetWithSelected[]>(targets);

    const [inputQuery, setInputQuery] = useState<string>("");
    // There must always be one item selected from the target list, e.g. auto-generated
    const selected = targets.find((t) => t._selected) as ITargetWithSelected;

    useEffect(() => {
        setItems(items);
    }, [targets]);

    /* Filter words on entering a search query in the target selection input field
       In source view mode this will also search in all target properties from all vocabularies.
     */
    useEffect(() => {
        if (!inputQuery) {
            setItems(targets);
        } else {
            const words = extractSearchWords(inputQuery, true);
            const filtered = targets.filter((o) => {
                const searchIn = `${o?.uri || ""} ${o?.label || ""} ${o?.description || ""}`.toLowerCase();
                return matchesAllWords(searchIn, words);
            });
            const existingUris = new Set(filtered.map((f) => f.uri));
            if (suggestVocabularyProperties && context.fetchTargetPropertySuggestions) {
                const timeout: number = window.setTimeout(async () => {
                    // For some reason the compiler cannot infer that context.fetchTargetPropertySuggestions is defined
                    if (context.fetchTargetPropertySuggestions) {
                        // Search in all target properties
                        const propertySuggestions = await context.fetchTargetPropertySuggestions(inputQuery);
                        const propertySuggestionsWithSelect = propertySuggestions
                            .filter((ps) => !existingUris.has(ps.uri))
                            .map((ps) => ({
                                ...ps,
                                _selected: false,
                            }));
                        setItems([...filtered, ...propertySuggestionsWithSelect]);
                    }
                }, 200);
                return () => clearTimeout(timeout);
            } else {
                setItems(filtered);
            }
        }
    }, [inputQuery]);

    const suggestVocabularyProperties = context.isFromDataset;

    const areTargetsEqual = (targetA: ITargetWithSelected, targetB: ITargetWithSelected) => {
        // Compare only the titles (ignoring case) just for simplicity.
        return targetA.uri?.toLowerCase() === targetB.uri?.toLowerCase();
    };

    const handleSelectTarget = (target: ITargetWithSelected) => {
        onChange(target);
    };

    const itemLabel = (target: ITargetWithSelected, search: string) => (
        <OverviewItem>
            <OverviewItemDescription>
                {target.label && (
                    <OverviewItemLine>
                        <OverflowText>
                            <Highlighter label={target.label} searchValue={search} />
                        </OverflowText>
                    </OverviewItemLine>
                )}
                {target.uri && (
                    <OverviewItemLine small={!!target.label ? true : false}>
                        <OverflowText ellipsis={"reverse"}>
                            <Highlighter label={target.uri} searchValue={search} />
                        </OverflowText>
                    </OverviewItemLine>
                )}
                {target.description && (
                    <OverviewItemLine small={true}>
                        <OverflowText>
                            <Highlighter label={target.description} searchValue={search} />
                        </OverflowText>
                    </OverviewItemLine>
                )}
            </OverviewItemDescription>
        </OverviewItem>
    );

    const itemRenderer = (target: ITargetWithSelected, { handleClick }) => {
        return (
            <MenuItem
                text={<div style={{ width: "40rem", maxWidth: "90vw" }}>{itemLabel(target, inputQuery)}</div>}
                key={target.uri}
                onClick={handleClick}
                active={target.uri === selected.uri}
            />
        );
    };

    const handleQueryChange = (value) => {
        if (!value) {
            setInputQuery("");
        } else {
            setInputQuery(value);
        }
    };

    return (
        <TargetSelect
            className={"ecc-silk-mapping__suggestionlist__target-select"}
            filterable={suggestVocabularyProperties || targets.length > 1}
            onItemSelect={handleSelectTarget}
            items={items}
            itemRenderer={itemRenderer}
            itemsEqual={areTargetsEqual}
            resetOnSelect={true}
            resetOnClose={true}
            inputProps={{
                placeholder: context.isFromDataset
                    ? "Enter text to search in all target properties..."
                    : "Filter candidates...",
                className: "ecc-silk-mapping__suggestionlist__target-property-search",
            }}
            contextOverlayProps={{
                minimal: true,
                popoverClassName: "ecc-silk-mapping__suggestionlist__target-dropdown",
                portalContainer: context.portalContainer,
            }}
            onQueryChange={handleQueryChange}
            query={inputQuery}
        >
            <Button fill={true} rightIcon="toggler-caret" text={itemLabel(selected, context.search)} />
        </TargetSelect>
    );
}
