import {
    FieldItem,
    Highlighter,
    highlighterUtils,
    MenuItem,
    MultiSuggestField,
    highlighterUtils,
    Spacing,
    MultiSuggestFieldCommonProps,
    OverflowText,
} from "@eccenca/gui-elements";
import React, { useEffect, useState } from "react";
import type { TagProps } from "@blueprintjs/core/src/components/tag/tag";
import { useTranslation } from "react-i18next";
import { IVocabularyInfo } from "./typings";
import useErrorHandler from "../../../hooks/useErrorHandler";

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
    const selectedVocabs = React.useRef<IVocabularyInfo[]>([]);
    const [t] = useTranslation();
    const { registerError } = useErrorHandler();
    const [selectedVocabs, setSelectedVocabs] = useState<IVocabularyInfo[]>([]);
    const [filteredVocabs, setFilteredVocabs] = useState<IVocabularyInfo[]>([]);
    const [searchQuery, setSearchQuery] = useState<string | undefined>(undefined);
    const [warning, setWarning] = React.useState<React.JSX.Element | null>(null);

    const availableVocabUris = new Set(availableVocabularies.map((v) => v.uri));

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

    const renderVocabulary: MultiSuggestFieldCommonProps<IVocabularyInfo>["itemRenderer"] = (
        vocabInfo: IVocabularyInfo,
        { modifiers, handleClick, query },
    ) => {
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

    const handleVocabSelect = (vocab: IVocabularyInfo) => {
        if (vocabSelected(vocab)) {
            removeVocabFromSelection(vocab.uri);
        } else {
            if (!availableVocabUris.has(vocab.uri)) {
                setWarning(
                    registerError(
                        "VocabularyMultiSelect_customVocabularyWarning",
                        t("widget.TargetVocabularySelection.customVocabularyWarning", { uri: vocab.uri }),
                        null,
                        {
                            intent: "warning",
                            errorNotificationInstanceId: "VocabularyMultiSelect",
                            onDismiss: () => setWarning(null),
                        },
                    ),
                );
            }
            setSelectedVocabs([...selectedVocabs, vocab]);
        }
    };

    const getTagProps = React.useCallback(
        (_value: string, index: number): TagProps => {
            const vocab = selectedVocabs[index];
            const isAvailable = vocab && availableVocabUris.has(vocab.uri);
            return {
                intent: isAvailable ? undefined : "warning",
                icon: isAvailable ? undefined : "warning-sign",
                htmlTitle: isAvailable ? undefined : t("widget.TargetVocabularySelection.notInstalledVocabulary"),
                minimal: true,
            };
        },
        [selectedVocabs, availableVocabUris, t],
    );

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
                    return items.filter((item) => {
                        const vocabLabel = item.label ? item.label : "";
                        const searchIn = `${vocabLabel} ${item.uri}`.toLowerCase();
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
                createNewItemFromQuery={allowCustomEntries ? createVocabularyFromQuery : undefined}
                newItemCreationText={allowCustomEntries ? "Add custom vocabulary" : undefined}
                inputProps={{
                    id: "vocselect",
                }}
                placeholder={"Select vocabularies..."}
                clearQueryOnSelection={true}
                createNewItemRenderer={newItemRenderer}
                noResults={<MenuItem disabled={true} text="No results." />}
                itemRenderer={renderVocabulary}
            />
            {warning ? (
                <>
                    <Spacing size={"small"} />
                    {warning}
                </>
            ) : null}
        </FieldItem>
    );
}
