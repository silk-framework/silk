import React from "react";
import { useDispatch, useSelector } from "react-redux";
import { workspaceOp, workspaceSel } from "@ducks/workspace";
import { commonSel } from "@ducks/common";
import { RadioButton, Spacing, TitleSubsection, Label } from "@eccenca/gui-elements";
import FacetsList from "./FacetsList";
import { useTranslation } from "react-i18next";
import { IAvailableDataTypeOption } from "@ducks/common/typings";

interface IFilterBarProps {
    extraItemTypeModifiers?: IAvailableDataTypeOption[];
    projectId?: string;
}

/** The filter menu that allows to filter the search results by selecting a specific item type and
 * filter by type specific facets. */
export function Filterbar({ extraItemTypeModifiers = [], projectId }: IFilterBarProps) {
    const dispatch = useDispatch();
    const [t] = useTranslation();

    const appliedFilters = useSelector(workspaceSel.appliedFiltersSelector);
    const modifiers = useSelector(commonSel.availableDTypesSelector);

    const typeModifier = modifiers.type;

    const handleFilterSelect = (field: string, val: string) => {
        let value = val !== appliedFilters[field] ? val : "";
        const filterOptions = {
            [field]: value,
        };
        dispatch(workspaceOp.applyFiltersOp(filterOptions));
        dispatch(workspaceOp.changePageOp(1));
    };

    return (
        <nav>
            {typeModifier && (
                <>
                    <TitleSubsection>
                        <Label
                            isLayoutForElement="h3"
                            text={t(`widget.Filterbar.subsections.titles.${typeModifier.field}`, typeModifier.label)}
                        />
                    </TitleSubsection>
                    <Spacing size="tiny" />
                    <ul data-test-id={"search-item-type-selection"}>
                        <li key={"alltypes"}>
                            <RadioButton
                                data-test-id={"item-type-radio-button-all"}
                                checked={!appliedFilters[typeModifier.field]}
                                label={t("common.messages.allTypes", "All types")}
                                onChange={() => handleFilterSelect(typeModifier.field, "")}
                                value={""}
                            />
                        </li>

                        {[...extraItemTypeModifiers, ...typeModifier.options]
                            .filter((mod) => !(!!projectId && (mod.label === "Project" || mod.label === "Global")))
                            .map((opt) => (
                                <li key={opt.id}>
                                    <RadioButton
                                        data-test-id={"item-type-radio-button-" + opt.id}
                                        checked={appliedFilters[typeModifier.field] === opt.id}
                                        label={t(
                                            `widget.Filterbar.subsections.valueLabels.itemType.${opt.id}`,
                                            opt.label
                                        )}
                                        onChange={() => handleFilterSelect(typeModifier.field, opt.id)}
                                        value={opt.id}
                                    />
                                </li>
                            ))}
                    </ul>
                    <Spacing />
                    <FacetsList projectId={projectId} />
                </>
            )}
        </nav>
    );
}
