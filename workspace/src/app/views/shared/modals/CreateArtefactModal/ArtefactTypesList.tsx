import React from "react";
import { useSelector } from "react-redux";
import { commonSel } from "@ducks/common";
import { Icon, Menu, MenuItem, TitleSubsection } from "@eccenca/gui-elements";
import { useTranslation } from "react-i18next";
import { createIconNameStack } from "../../ItemDepiction/ItemDepiction";
import { itemTypeTileClass } from "../../ItemDepiction/itemTypeColors";

interface Props {
    onSelect: (id: string) => any;
    /** Blacklist for types. Set of type IDs. */
    typesToRemove: Set<string>;
}

/** Item-type icon in a small color-coded tile, matching the depiction tiles of the plugin cards
 * on the right (same `itemTypeTileClass` palette as the type badges). */
const TypeIconTile = ({ iconNames, itemType }: { iconNames: string[]; itemType?: string }) => (
    <span
        className={`flex size-6 shrink-0 items-center justify-center rounded-md ${
            (itemType && itemTypeTileClass(itemType)) || "bg-muted text-muted-foreground"
        }`}
    >
        <Icon name={iconNames} small />
    </span>
);

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
                    icon={<TypeIconTile iconNames={["application-dataintegration"]} />}
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
                                icon={<TypeIconTile iconNames={createIconNameStack(type.id)} itemType={type.id} />}
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
