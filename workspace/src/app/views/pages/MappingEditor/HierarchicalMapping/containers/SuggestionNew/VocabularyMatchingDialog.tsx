import {
    Button,
    FieldItem,
    Highlighter,
    MenuItem,
    SimpleDialog,
    MultiSuggestField,
    highlighterUtils,
} from "@eccenca/gui-elements";
import React, { useContext, useEffect, useState } from "react";
import { IVocabularyInfo } from "./suggestion.typings";
import { SuggestionListContext } from "./SuggestionContainer";

interface IProps {
    // The available vocabularies to match against
    availableVocabularies: IVocabularyInfo[];
    // Execute vocabulary matching with the selected vocabularies. Empty array means "match over all vocabularies".
    executeMatching: (vocabFilter: string[]) => any;
    // Executed when the 'Cancel' button is clicked
    onClose: () => void;
    // Optional preselection of vocabularies by URI
    preselection?: string[];
    // Callback when the selection changes. This is e.g. used to cache the last selection externally.
    onSelection?: (selectedVocabs: IVocabularyInfo[]) => any;
}

const VocabularyMultiSelect = MultiSuggestField.ofType<IVocabularyInfo>();

const vocabLabel = (vocabInfo: IVocabularyInfo) => {
    return vocabInfo.label ? vocabInfo.label : vocabInfo.uri;
};

/** Vocabulary matching dialog that allows to match against a subset of vocabularies. */
export default function VocabularyMatchingDialog({
    availableVocabularies,
    executeMatching,
    onClose,
    onSelection,
    preselection,
}: IProps) {
    const context = useContext(SuggestionListContext);
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

    const handleCancel = () => {
        onSelection && onSelection(preselectedVocabs);
        onClose();
    };

    const clearButton =
        selectedVocabs.length > 0 ? (
            <Button icon="operation-clear" data-test-id="clear-all-vocabs" minimal={true} onClick={handleClear} />
        ) : undefined;

    const onQueryChange = (query: string) => {
        setSearchQuery(query);
    };

    return (
        <SimpleDialog
            portalContainer={context.portalContainer}
            title={"Refine matching options"}
            isOpen={true}
            preventSimpleClosing={true}
            canOutsideClickClose={true}
            onClose={handleCancel}
            actions={[
                <Button
                    key="match"
                    affirmative
                    onClick={() => {
                        onClose();
                        executeMatching(selectedVocabs.map((vocab) => vocab.uri));
                    }}
                    data-test-id={"vocab-match-execute-btn"}
                >
                    Confirm selection
                </Button>,
                <Button key="cancel" onClick={handleCancel}>
                    Cancel
                </Button>,
            ]}
        >
            <FieldItem
                labelProps={{
                    text: "Select vocabularies used to match data attributes",
                    htmlFor: "vocselect",
                }}
            >
                <VocabularyMultiSelect
                    popoverProps={{
                        portalContainer: context.portalContainer,
                        minimal: true,
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
                            autoComplete: "off",
                        },
                        onRemove: removeVocabFromSelectionViaIndex,
                        rightElement: clearButton,
                        tagProps: { minimal: true },
                    }}
                    selectedItems={selectedVocabs}
                />
            </FieldItem>
        </SimpleDialog>
    );
}
