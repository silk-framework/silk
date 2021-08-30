import React, { useEffect, useState } from "react";
import { FieldItem, FieldItemRow, RadioButton } from "@gui-elements/index";
import { IVocabularyInfo } from "./typings";
import VocabularyMultiSelect from "./VocabularyMultiSelect";
import { useTranslation } from "react-i18next";

interface ITargetVocabularySelectionProps {
    // The static entries, one value per radio button
    staticEntries: StaticVocabularyEntry[];

    // The initially selected item
    selectedItem: SelectedVocabularyItem;

    // The config for the multi-selection widget. When selected, allows to multi-select from the given vocabularies.
    multiSelection?: {
        // The label of the radio button menu point for choosing multi-selection
        label: string;
        // The vocabularies from which the user can choose from
        vocabularies: IVocabularyInfo[];
        // The initial selection of vocabularies
        initialSelection: string[];
    };
    // Called when the selection changes. In case of multi-select, vocabulary values/URIs are concatenated with ','.
    onChange: (value: string) => any;
}

export type StaticVocabularyEntry = {
    label: string;
    value: string;
};

const initialValue = (props: ITargetVocabularySelectionProps): string => {
    const { multiSelection, selectedItem } = props;
    if (selectedItem.multiSelection && multiSelection) {
        return multiSelection.initialSelection.join(",");
    } else if (!selectedItem.multiSelection) {
        return selectedItem.staticValue;
    } else {
        throw new Error("No multi-selection config specified, but initial selection set to multi-select!");
    }
};

/** Allows to select either static pre-selections or manually picked vocabularies from a set of vocabularies. */
export function TargetVocabularySelection(props: ITargetVocabularySelectionProps) {
    const { multiSelection, onChange } = props;
    const [value, setValue] = useState<string>(initialValue(props));
    const [showMultiSelect, setShowMultiSelect] = useState<boolean>(props.selectedItem.multiSelection);
    const [t] = useTranslation();

    useEffect(() => {
        onChange(value);
    }, [value]);

    const handleOnChange = (selectedMenuItem: SelectedVocabularyItem) => {
        if (selectedMenuItem.multiSelection && multiSelection) {
            setShowMultiSelect((old) => {
                if (!old) {
                    // Reset multi-select value when multi-select widget was not selected before
                    setValue(multiSelection.initialSelection.join(","));
                }
                return true;
            });
        } else if (!selectedMenuItem.multiSelection) {
            setShowMultiSelect(false);
            setValue(selectedMenuItem.staticValue);
        }
    };

    return (
        <FieldItem>
            <TargetVocabularyRadioMenu selectionConfig={props} onChange={handleOnChange} />
            {showMultiSelect && (
                <VocabularyMultiSelect
                    label={t("widget.TargetVocabularySelection.multiSelectionRadioButtonLabel")}
                    availableVocabularies={props.multiSelection?.vocabularies || []}
                    preselection={props.multiSelection?.initialSelection}
                    onSelection={(vocabs) => setValue(vocabs.map((v) => v.uri).join(","))}
                />
            )}
        </FieldItem>
    );
}

interface ITargetVocabularyRadioMenuProps {
    selectionConfig: ITargetVocabularySelectionProps;
    onChange: (item: SelectedVocabularyItem) => any;
}

export type SelectedVocabularyItem =
    | {
          // Static value selected
          multiSelection: false;
          staticValue: string;
      }
    | {
          // Multi-selection selected
          multiSelection: true;
      };

/** The radio button menu to either choose one of the static entries or to allow to manually pick vocabularies from a given set. */
function TargetVocabularyRadioMenu({ selectionConfig, onChange }: ITargetVocabularyRadioMenuProps) {
    const [selectedItem, setSelectedItem] = useState<SelectedVocabularyItem>(selectionConfig.selectedItem);
    const handleChange = (item: SelectedVocabularyItem) => {
        setSelectedItem(item);
        onChange(item);
    };
    return (
        <FieldItemRow>
            {selectionConfig.staticEntries.map(({ label, value }) => (
                <FieldItem key={value}>
                    <RadioButton
                        checked={!selectedItem.multiSelection && selectedItem.staticValue === value}
                        label={label}
                        onChange={() => handleChange({ multiSelection: false, staticValue: value })}
                        value={value}
                    />
                </FieldItem>
            ))}
            {selectionConfig.multiSelection && (
                <RadioButton
                    checked={selectedItem.multiSelection}
                    label={selectionConfig.multiSelection.label}
                    onChange={() => handleChange({ multiSelection: true })}
                    value={"__multi_selection__"}
                />
            )}
        </FieldItemRow>
    );
}
