import {Button, FieldItem, MenuItem, Select, SimpleDialog} from "@gui-elements/index";
import React, {useContext, useState} from "react";
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
}

const VocabularyMultiSelect = MultiSelect.ofType<IVocabularyInfo>();

const vocabLabel = (vocabInfo: IVocabularyInfo) => vocabInfo.label ? vocabInfo.label : vocabInfo.uri

/** Vocabulary matching dialog that allows to match against a subset of vocabularies. */
export default function VocabularyMatchingDialog({availableVocabularies, executeMatching, onClose}: IProps) {
    const context = useContext(SuggestionListContext);
    const [selectedVocabs, setSelectedVocabs] = useState<IVocabularyInfo[]>([])

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

    const handleVocabSelect = (vocab: IVocabularyInfo) => {
        if(vocabSelected(vocab)) {
            setSelectedVocabs(selectedVocabs.filter((v) => v.uri !== vocab.uri))
        } else {
            setSelectedVocabs([...selectedVocabs, vocab])
        }
    }
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
        <VocabularyMultiSelect
            popoverProps={{portalContainer: context.portalContainer}}
            //            {...filmSelectProps}
            //{...flags}
            itemRenderer={renderVocabulary}
            itemsEqual={((a, b) => a.uri === b.uri)}
            items={availableVocabularies}
            noResults={<MenuItem disabled={true} text="No results." />}
            onItemSelect={handleVocabSelect}
            //popoverProps={{}}
            tagRenderer={(vocab) => vocabLabel(vocab)}
//            tagInputProps={{
//                onRemove: this.handleTagRemove,
//                rightElement: clearButton,
//                tagProps: getTagProps,
//            }}
            selectedItems={selectedVocabs}
            />
    </SimpleDialog>
}