import { Button, FieldItem, Highlighter, MenuItem } from "@gui-elements/index";
import React, { useEffect, useState } from "react";
import { MultiSelect } from "@blueprintjs/select";
import { extractSearchWords, matchesAllWords } from "@gui-elements/src/components/Typography/Highlighter";
import { IVocabularyInfo } from "./typings";

interface IProps {
    // Label for this widget
    label: string;
    // The available vocabularies to match against
    availableVocabularies: IVocabularyInfo[];
    // Optional preselection of vocabularies by URI
    preselection?: string[];
    // Callback when the selection changes. This is e.g. used to cache the last selection externally.
    onSelection?: (selectedVocabs: IVocabularyInfo[]) => any;
}

const VocabularyMultiSelectBP = MultiSelect.ofType<IVocabularyInfo>();

const vocabLabel = (vocabInfo: IVocabularyInfo) => {
    return vocabInfo.label ? vocabInfo.label : vocabInfo.uri;
};

/** Vocabulary multi-select component. */
export default function VocabularyMultiSelect({ availableVocabularies, onSelection, preselection, label }: IProps) {
    const [selectedVocabs, setSelectedVocabs] = useState<IVocabularyInfo[]>([]);
    const [filteredVocabs, setFilteredVocabs] = useState<IVocabularyInfo[]>([]);
    const [searchQuery, setSearchQuery] = useState<string | undefined>(undefined);
    const [preselectedVocabs, setPreselectedVocabs] = useState<IVocabularyInfo[]>([]);

    const preselect = () =>
        preselection ? availableVocabularies.filter((v) => preselection.includes(v.uri)) : availableVocabularies;

    useEffect(() => {
        if (preselection) {
            setSelectedVocabs(preselect());
            setPreselectedVocabs(preselect());
        }
    }, []);

    useEffect(() => {
        if (onSelection) {
            onSelection(selectedVocabs);
        }
    }, [selectedVocabs.map((v) => v.uri).join(",")]);

    useEffect(() => {
        if (searchQuery) {
            const searchWords = extractSearchWords(searchQuery, true);
            const filtered = availableVocabularies.filter((vocab) => {
                const vocabLabel = vocab.label ? vocab.label : "";
                const searchIn = `${vocabLabel} ${vocab.uri}`.toLowerCase();
                return matchesAllWords(searchIn, searchWords);
            });
            setFilteredVocabs(filtered);
        } else {
            setFilteredVocabs(availableVocabularies);
        }
    }, [searchQuery]);

    // Renders the entries of the (search) options list
    const optionRenderer = (vocabInfo: IVocabularyInfo) => {
        return <Highlighter label={vocabLabel(vocabInfo)} searchValue={searchQuery} />;
    };

    const vocabSelected = (vocab: IVocabularyInfo): boolean => {
        return selectedVocabs.some((v) => v.uri === vocab.uri);
    };

    const renderVocabulary = (vocabInfo: IVocabularyInfo, { modifiers, handleClick }) => {
        return (
            <MenuItem
                icon={vocabSelected(vocabInfo) ? "state-checked" : "state-unchecked"}
                active={modifiers.active}
                key={vocabInfo.uri}
                label={"property count: " + vocabInfo.nrProperties}
                onClick={handleClick}
                text={optionRenderer(vocabInfo)}
                shouldDismissPopover={false}
            />
        );
    };

    const removeVocabFromSelection = (vocabUri: string) => {
        setSelectedVocabs(selectedVocabs.filter((v) => v.uri !== vocabUri));
    };

    const removeVocabFromSelectionViaIndex = (vocabLabel: string, index: number) => {
        setSelectedVocabs([...selectedVocabs.slice(0, index), ...selectedVocabs.slice(index + 1)]);
    };

    const handleVocabSelect = (vocab: IVocabularyInfo) => {
        if (vocabSelected(vocab)) {
            removeVocabFromSelection(vocab.uri);
        } else {
            setSelectedVocabs([...selectedVocabs, vocab]);
        }
    };

    const handleClear = () => {
        setSelectedVocabs([]);
    };

    const clearButton =
        selectedVocabs.length > 0 ? (
            <Button icon="operation-clear" data-test-id="clear-all-vocabs" minimal={true} onClick={handleClear} />
        ) : undefined;

    const onQueryChange = (query: string) => {
        setSearchQuery(query);
    };

    return (
        <FieldItem
            labelAttributes={{
                text: label,
            }}
            // hasStateDanger={hasError} TODO?
            // messageText={hasError ? validationErrorText : undefined} TODO?
        >
            <VocabularyMultiSelectBP
                popoverProps={{
                    // portalContainer: context.portalContainer,
                    minimal: true,
                    fill: true,
                    position: "bottom-left",
                }}
                fill={true}
                onQueryChange={onQueryChange}
                itemRenderer={renderVocabulary}
                itemsEqual={(a, b) => a.uri === b.uri}
                items={filteredVocabs}
                noResults={<MenuItem disabled={true} text="No results." />}
                onItemSelect={handleVocabSelect}
                tagRenderer={(vocab) => vocabLabel(vocab)}
                tagInputProps={{
                    inputProps: {
                        id: "vocselect",
                        autocomplete: "off",
                    },
                    onRemove: removeVocabFromSelectionViaIndex,
                    rightElement: clearButton,
                    tagProps: { minimal: true },
                }}
                selectedItems={selectedVocabs}
            />
        </FieldItem>
    );
}
