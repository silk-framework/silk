import {Button, FieldItem, Label, MenuItem, Select, SimpleDialog} from "@gui-elements/index";
import React, {useContext, useEffect, useState} from "react";
import {IVocabularyInfo} from "./suggestion.typings";
import {SuggestionListContext} from "./SuggestionContainer";
import {MultiSelect} from "@blueprintjs/select";

interface IProps {
    // The available vocabularies to match against
    availableVocabularies: IVocabularyInfo[]
    // Execute vocabulary matching with the selected vocabularies. Empty array means "match over all vocabularies".
    executeMatching: (vocabFilter: string[]) => any
    // Executed when the 'Cancel' button is clicked
    onClose: () => void
    // Optional preselection of vocabularies by URI
    preselection?: string[]
    // Callback when the selection changes. This is e.g. used to cache the last selection externally.
    onSelection?: (selectedVocabs: IVocabularyInfo[]) => any
}

const VocabularyMultiSelect = MultiSelect.ofType<IVocabularyInfo>();

const vocabLabel = (vocabInfo: IVocabularyInfo) => vocabInfo.label ? vocabInfo.label : vocabInfo.uri

/** Vocabulary matching dialog that allows to match against a subset of vocabularies. */
export default function VocabularyMatchingDialog(
    {
        availableVocabularies, executeMatching, onClose, onSelection, preselection
    }: IProps) {
    const context = useContext(SuggestionListContext);
    const [selectedVocabs, setSelectedVocabs] = useState<IVocabularyInfo[]>([])

    useEffect(() => {
        if(preselection) {
            setSelectedVocabs(availableVocabularies.filter((v) => preselection.includes(v.uri)))
        }
    }, [])

    useEffect(() => {
        if(onSelection) {
            onSelection(selectedVocabs)
        }
    }, [selectedVocabs.map((v) => v.uri).join(",")])

    const vocabSelected = (vocab: IVocabularyInfo): boolean => {
        return selectedVocabs.some((v) => v.uri === vocab.uri)
    }

    const renderVocabulary = (vocabInfo: IVocabularyInfo, { modifiers, handleClick }) => {
        return <MenuItem
            icon={vocabSelected(vocabInfo) && "item-remove"}
            active={modifiers.active}
            key={vocabInfo.uri}
            label={"property count: " + vocabInfo.nrProperties}
            onClick={handleClick}
            text={vocabLabel(vocabInfo)}
            shouldDismissPopover={false}
        />
    }

    const removeVocabFromSelection = (vocabUri: string) => {
        setSelectedVocabs(selectedVocabs.filter((v) => v.uri !== vocabUri))
    }

    const handleVocabSelect = (vocab: IVocabularyInfo) => {
        if(vocabSelected(vocab)) {
            removeVocabFromSelection(vocab.uri)
        } else {
            setSelectedVocabs([...selectedVocabs, vocab])
        }
    }

    const handleVocabRemove = (vocabUri) => {
        removeVocabFromSelection(vocabUri)
    }

    const handleClear = () => {
        setSelectedVocabs([])
    }

    const clearButton =
        selectedVocabs.length > 0 ? <Button icon="operation-clear" minimal={true} onClick={handleClear} /> : undefined;

    return <SimpleDialog
        portalContainer={context.portalContainer}
        size="small"
        title={"Vocabulary matching"}
        isOpen={true}
        onClose={onClose}
        actions={[
            <Button
                key="match"
                affirmative
                onClick={() => {
                    onClose()
                    executeMatching(selectedVocabs.map((vocab) => vocab.uri))}
                }
                data-test-id={"vocab-match-execute-btn"}
            >
                Match
            </Button>,
            <Button key="cancel" onClick={onClose}>
                Cancel
            </Button>,
        ]}>
        <p>Select vocabularies:</p>
        <VocabularyMultiSelect
            popoverProps={{
                portalContainer: context.portalContainer,
                minimal: true,
                fill: true,
                position: "bottom-left"
            }}
            fill={true}
            itemRenderer={renderVocabulary}
            itemsEqual={((a, b) => a.uri === b.uri)}
            items={availableVocabularies}
            noResults={<MenuItem disabled={true} text="No results."/>}
            onItemSelect={handleVocabSelect}
            tagRenderer={(vocab) => vocabLabel(vocab)}
            tagInputProps={{
                onRemove: handleVocabRemove,
                rightElement: clearButton,
                tagProps: {minimal: true},
            }}
            selectedItems={selectedVocabs}
        />
    </SimpleDialog>
}