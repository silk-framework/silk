import {
    Button,
    FieldItem,
    highlighterUtils,
    MenuItem,
    MultiSuggestField,
    MultiSuggestFieldCommonProps,
    SimpleDialog,
} from "@eccenca/gui-elements";
import React, {useContext, useEffect, useState} from "react";
import {IVocabularyInfo} from "./suggestion.typings";
import {SuggestionListContext} from "./SuggestionContainer";
import {
    vocabularyLabel,
    vocabularyOptionRenderer
} from "../../../../../shared/TargetVocabularySelection/VocabularyMultiSelect";

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

/** Vocabulary matching dialog that allows to match against a subset of vocabularies. */
export default function VocabularyMatchingDialog({
    availableVocabularies,
    executeMatching,
    onClose,
    onSelection,
    preselection,
}: IProps) {
    const context = useContext(SuggestionListContext);
    const selectedVocabs = React.useRef<IVocabularyInfo[]>([]);
    const [preselectedVocabs, setPreselectedVocabs] = useState<IVocabularyInfo[]>([]);

    const preselect = () =>
        preselection ? availableVocabularies.filter((v) => preselection.includes(v.uri)) : availableVocabularies;

    useEffect(() => {
        if (preselection) {
            setPreselectedVocabs(preselect());
        }
    }, []);

    const vocabSelected = (vocab: IVocabularyInfo): boolean => {
        return selectedVocabs.current.some((v) => v.uri === vocab.uri);
    };

    const renderVocabulary: MultiSuggestFieldCommonProps<IVocabularyInfo>["itemRenderer"] = (vocabInfo: IVocabularyInfo, { modifiers, handleClick, query }) => {
        return (
            <MenuItem
                icon={vocabSelected(vocabInfo) ? "state-checked" : "state-unchecked"}
                active={modifiers.active}
                key={vocabInfo.uri}
                label={"property count: " + vocabInfo.nrProperties}
                onClick={handleClick}
                text={vocabularyOptionRenderer(vocabInfo, query)}
                shouldDismissPopover={false}
            />
        );
    };

    const handleCancel = () => {
        onSelection && onSelection(preselectedVocabs);
        onClose();
    };

    return (
        <SimpleDialog
            portalContainer={context.portalContainer}
            size="regular"
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
                        executeMatching(selectedVocabs.current.map((vocab) => vocab.uri));
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
                <MultiSuggestField<IVocabularyInfo>
                    itemId={(vocab) => vocab.uri}
                    itemLabel={(vocab) => vocabularyLabel(vocab)}
                    items={availableVocabularies}
                    searchListPredicate={(items, query) => {
                        const searchWords = highlighterUtils.extractSearchWords(query, true);
                        return items.filter((vocab) => {
                            const vocabLabel = vocab.label ? vocab.label : "";
                            const searchIn = `${vocabLabel} ${vocab.uri}`.toLowerCase();
                            return highlighterUtils.matchesAllWords(searchIn, searchWords);
                        });
                    }}
                    selectedItems={preselectedVocabs}
                    onSelection={(selection) => {
                        selectedVocabs.current = selection.selectedItems;
                        if (onSelection) {
                            onSelection(selection.selectedItems);
                        }
                    }}
                    inputProps={{
                        id: "vocselect"
                    }}
                    placeholder={"Select vocabularies..."}
                    clearQueryOnSelection={true}
                    noResults={<MenuItem disabled={true} text="No results." />}
                    itemRenderer={renderVocabulary}
                    contextOverlayProps={{
                        portalContainer: context.portalContainer
                    }}
                />
            </FieldItem>
        </SimpleDialog>
    );
}
