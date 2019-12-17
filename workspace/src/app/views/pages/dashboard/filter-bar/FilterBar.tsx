import React, { useEffect } from "react";
import { useDispatch, useSelector } from "react-redux";
import { dashboardOp, dashboardSel } from "@ducks/dashboard";
import Checkbox from "@wrappers/checkbox";
import Label from "@wrappers/label";
import FacetsList from "./FacetsList";

export default function FilterBar() {
    const dispatch = useDispatch();

    const appliedFilters = useSelector(dashboardSel.appliedFiltersSelector);
    const modifiers = useSelector(dashboardSel.modifiersSelector);

    const typeModifier = modifiers.type;

    useEffect(() => {
        dispatch(dashboardOp.fetchTypesAsync());
    }, []);

    const handleFilterSelect = (field: string, val: string) => {
        let value = val !== appliedFilters[field] ? val : '';
        dispatch(dashboardOp.applyFilter({
            field,
            value
        }));
    };

    return (
        <aside>
            {
                typeModifier &&
                <>
                    <Label>{typeModifier.label}</Label>
                    {
                        typeModifier.options.map(opt =>
                            <Checkbox
                                checked={appliedFilters[typeModifier.field] === opt.id}
                                label={opt.id}
                                onChange={() => handleFilterSelect(typeModifier.field, opt.id)}
                                value={opt.id}
                                key={opt.id}
                            />
                        )
                    }

                    <FacetsList/>
                </>
            }
        </aside>
    )
}
