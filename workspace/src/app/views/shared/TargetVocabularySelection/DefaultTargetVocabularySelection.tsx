import { SelectedVocabularyItem, StaticVocabularyEntry, TargetVocabularySelection } from "./TargetVocabularySelection";
import React, { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { requestGlobalVocabularies } from "./requests";
import { IVocabularyInfo } from "./typings";
import Loading from "../Loading";
import { IInputAttributes } from "../modals/CreateArtefactModal/ArtefactForms/InputMapper";

interface IProps {
    onChange: (value: string) => any;
    initialValue?: string;
}

/** Target vocabulary selection component that has static entries 'all installed vocabularies' and 'none'.
 *  And alternatively allows to multi-select all available vocabularies (from the global vocabulary cache). */
export function DefaultTargetVocabularySelection({ id, name, intent, defaultValue, onChange }: IInputAttributes) {
    const [vocabularies, setVocabularies] = useState<IVocabularyInfo[]>([]);
    const [loading, setLoading] = useState<boolean>(true);
    const [t] = useTranslation();
    useEffect(() => {
        fetchVocabularies();
    }, []);
    const fetchVocabularies = async () => {
        setLoading(true);
        try {
            const vocabs = await requestGlobalVocabularies();
            setVocabularies(vocabs.data.vocabularies);
        } catch {
            // TODO: error handling
        } finally {
            setLoading(false);
        }
    };
    const staticEntries: StaticVocabularyEntry[] = [
        {
            label: t("widget.TargetVocabularySelection.staticEntryLabel.all"),
            value: "all installed vocabularies",
        },
        {
            label: t("widget.TargetVocabularySelection.staticEntryLabel.none"),
            value: "no vocabularies",
        },
    ];
    const initialStaticEntry = !defaultValue || staticEntries.map((entry) => entry.value).includes(defaultValue || "");
    const selectedItem: SelectedVocabularyItem = initialStaticEntry
        ? {
              multiSelection: false,
              staticValue: defaultValue ? defaultValue : staticEntries[0].value,
          }
        : {
              multiSelection: true,
          };

    return loading ? (
        <Loading />
    ) : (
        <TargetVocabularySelection
            staticEntries={staticEntries}
            selectedItem={selectedItem}
            multiSelection={{
                label: t("widget.TargetVocabularySelection.multiSelectionRadioButtonLabel"),
                initialSelection: !initialStaticEntry ? (defaultValue || "").trim().split(",") : [],
                vocabularies,
            }}
            onChange={onChange}
        />
    );
}
