import React from "react";
import { useSelector } from "react-redux";
import { commonSel } from "@ducks/common";
import { Menu, MenuItem, TitleSubsection } from "@eccenca/gui-elements";
import { useTranslation } from "react-i18next";

interface Props {
    onSelect: (id: string) => any;
    /** Blacklist for types. Set of type IDs. */
    typesToRemove: Set<string>;
}
/** Shows the item categories on the left side of the item type selection dialog. */
function ArtefactTypesList({ onSelect, typesToRemove }: Props) {
    const { selectedDType } = useSelector(commonSel.artefactModalSelector);
    const typeModifier = useSelector(commonSel.availableDTypesSelector).type;

    const [t] = useTranslation();
    return (
        <>
            <TitleSubsection>{t("common.words.itemType", "Item type")}</TitleSubsection>
            <Menu>
                <MenuItem
                    text={t("common.words.all", "All")}
                    key="all"
                    onClick={() => onSelect("all")}
                    active={selectedDType === "all"}
                />
                {typeModifier &&
                    typeModifier.options
                        .filter((type) => !typesToRemove.has(type.id))
                        .map((type) => (
                            <MenuItem
                                text={type.label}
                                key={type.id}
                                onClick={() => onSelect(type.id)}
                                active={selectedDType === type.id}
                                data-test-id={`item-type-${type.id}`}
                            />
                        ))}
            </Menu>
        </>
    );
}

export default ArtefactTypesList;
