import {
    Button,
    FieldItem,
    Highlighter,
    MenuItem,
    OverflowText,
    MultiSuggestField,
    highlighterUtils,
} from "@eccenca/gui-elements";
import React, { useEffect, useState } from "react";
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
    // If it should be possible in the multi-select to add custom entries
    allowCustomEntries?: boolean;
}

const VocabularyMultiSelectBP = MultiSuggestField.ofType<IVocabularyInfo>();

const vocabLabel = (vocabInfo: IVocabularyInfo) => {
    return vocabInfo.label ? vocabInfo.label : vocabInfo.uri;
};

/** Vocabulary multi-select component. */
export default function VocabularyMultiSelect({
    availableVocabularies,
    onSelection,
    preselection,
    label,
    allowCustomEntries,
}: IProps) {
    const [selectedVocabs, setSelectedVocabs] = useState<IVocabularyInfo[]>([]);
    const [filteredVocabs, setFilteredVocabs] = useState<IVocabularyInfo[]>([]);
    const [searchQuery, setSearchQuery] = useState<string | undefined>(undefined);

    const preselect = (): IVocabularyInfo[] => {
        const vocabMap = new Map<string, IVocabularyInfo>(availableVocabularies.map((vocab) => [vocab.uri, vocab]));
        return preselection
            ? preselection.map((vocabUri) => {
                  return vocabMap.has(vocabUri)
                      ? (vocabMap.get(vocabUri) as IVocabularyInfo)
                      : {
                            uri: vocabUri,
                            label: vocabUri,
                        };
              })
            : [];
    };

    useEffect(() => {
        if (preselection) {
            setSelectedVocabs(preselect());
        }
    }, []);

    useEffect(() => {
        if (onSelection) {
            onSelection(selectedVocabs);
        }
    }, [selectedVocabs.map((v) => v.uri).join(",")]);

    useEffect(() => {
        if (searchQuery) {
            const searchWords = highlighterUtils.extractSearchWords(searchQuery, true);
            const filtered = availableVocabularies.filter((vocab) => {
                const vocabLabel = vocab.label ? vocab.label : "";
                const searchIn = `${vocabLabel} ${vocab.uri}`.toLowerCase();
                return highlighterUtils.matchesAllWords(searchIn, searchWords);
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

    const vocabInfoString = (vocabInfo: IVocabularyInfo): string => {
        const classInfo = vocabInfo.nrClasses ? `${vocabInfo.nrClasses} classes` : "";
        const propertyInfo = vocabInfo.nrProperties ? `${vocabInfo.nrProperties} properties` : "";
        const infix = classInfo && propertyInfo ? ", " : "";
        return classInfo || propertyInfo ? `(${classInfo}${infix}${propertyInfo})` : "";
    };

    const renderVocabulary = (vocabInfo: IVocabularyInfo, { modifiers, handleClick }) => {
        return (
            <MenuItem
                icon={vocabSelected(vocabInfo) ? "state-checked" : "state-unchecked"}
                active={modifiers.active}
                key={vocabInfo.uri}
                label={vocabInfoString(vocabInfo)}
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

    const illegalCharsRegex = /\s|,|<|>/;

    const createVocabularyFromQuery = (query: string): IVocabularyInfo => {
        return {
            uri: query,
            label: query,
        };
    };

    const newItemRenderer = (query: string, active: boolean, handleClick) => {
        // Lightweight test to check if the query could be a valid URI or file name
        if (
            allowCustomEntries &&
            !illegalCharsRegex.test(query) &&
            (query.indexOf(":") > 0 || query.indexOf(".") > 0)
        ) {
            return (
                <MenuItem
                    id={"new-vocab-item"}
                    icon={"item-add-artefact"}
                    active={active}
                    key={query}
                    onClick={handleClick}
                    text={<OverflowText>{`Add vocabulary URI '${query}'`}</OverflowText>}
                />
            );
        }
    };

    return (
        <FieldItem
            labelProps={{
                text: label,
            }}
            // hasStateDanger={hasError} TODO?
            // messageText={hasError ? validationErrorText : undefined} TODO?
        >
            <VocabularyMultiSelectBP
                popoverProps={{
                    minimal: true,
                    placement: "bottom-start",
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
                        autoComplete: "off",
                    },
                    onRemove: removeVocabFromSelectionViaIndex,
                    rightElement: clearButton,
                    tagProps: { minimal: true },
                }}
                selectedItems={selectedVocabs}
                createNewItemRenderer={newItemRenderer}
                createNewItemFromQuery={createVocabularyFromQuery}
            />
        </FieldItem>
    );
}
