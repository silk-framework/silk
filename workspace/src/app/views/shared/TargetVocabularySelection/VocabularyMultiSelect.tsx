import {
    FieldItem,
    Highlighter,
    highlighterUtils,
    MenuItem,
    MultiSuggestField,
    MultiSuggestFieldCommonProps,
    OverflowText,
} from "@eccenca/gui-elements";
import React, {useEffect, useState} from "react";
import {IVocabularyInfo} from "./typings";

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

export const vocabularyLabel = (vocabInfo: IVocabularyInfo) => {
    return vocabInfo.label ? vocabInfo.label : vocabInfo.uri;
};

// Renders the entries of the (search) options list
export const vocabularyOptionRenderer = (vocabInfo: IVocabularyInfo, query: string) => {
    return <Highlighter label={vocabularyLabel(vocabInfo)} searchValue={query} />;
};

/** Vocabulary multi-select component. */
export default function VocabularyMultiSelect({
    availableVocabularies,
    onSelection,
    preselection,
    label,
    allowCustomEntries,
}: IProps) {
    const [preselectedVocabs, setPreselectedVocabs] = useState<IVocabularyInfo[]>([]);
    const selectedVocabs = React.useRef<IVocabularyInfo[]>([])

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
            setPreselectedVocabs(preselect());
        }
    }, []);

    const vocabSelected = (vocab: IVocabularyInfo): boolean => {
        return selectedVocabs.current.some((v) => v.uri === vocab.uri);
    };

    const vocabInfoString = (vocabInfo: IVocabularyInfo): string => {
        const classInfo = vocabInfo.nrClasses ? `${vocabInfo.nrClasses} classes` : "";
        const propertyInfo = vocabInfo.nrProperties ? `${vocabInfo.nrProperties} properties` : "";
        const infix = classInfo && propertyInfo ? ", " : "";
        return classInfo || propertyInfo ? `(${classInfo}${infix}${propertyInfo})` : "";
    };

    const renderVocabulary: MultiSuggestFieldCommonProps<IVocabularyInfo>["itemRenderer"] = (vocabInfo: IVocabularyInfo, { modifiers, handleClick, query }) => {
        return (
            <MenuItem
                icon={vocabSelected(vocabInfo) ? "state-checked" : "state-unchecked"}
                active={modifiers.active}
                key={vocabInfo.uri}
                label={vocabInfoString(vocabInfo)}
                onClick={handleClick}
                text={vocabularyOptionRenderer(vocabInfo, query)}
                shouldDismissPopover={false}
            />
        );
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
        >
            <MultiSuggestField<IVocabularyInfo>
                itemId={(vocab) => vocab.uri}
                itemLabel={(vocab) => vocabularyLabel(vocab)}
                items={availableVocabularies}
                searchListPredicate={(items, query) => {
                    const searchWords = highlighterUtils.extractSearchWords(query, true);
                    return items.filter(item => {
                        const vocabLabel = item.label ? item.label : "";
                        const searchIn = `${vocabLabel} ${item.uri}`.toLowerCase();
                        return highlighterUtils.matchesAllWords(searchIn, searchWords);
                    })
                }}
                selectedItems={preselectedVocabs}
                onSelection={(selection) => {
                    selectedVocabs.current = selection.selectedItems;
                    if (onSelection) {
                        onSelection(selection.selectedItems);
                    }
                }}
                createNewItemFromQuery={allowCustomEntries ? createVocabularyFromQuery : undefined}
                newItemCreationText={allowCustomEntries ? "Add custom vocabulary" : undefined}
                inputProps={{
                    id: "vocselect"
                }}
                placeholder={"Select vocabularies..."}
                clearQueryOnSelection={true}
                createNewItemRenderer={newItemRenderer}
                noResults={<MenuItem disabled={true} text="No results." />}
                itemRenderer={renderVocabulary}
            />
        </FieldItem>
    );
}
