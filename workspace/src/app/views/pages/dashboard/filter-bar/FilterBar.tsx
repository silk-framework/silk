import React, { useEffect } from "react";
import { useDispatch, useSelector } from "react-redux";
import { dashboardOp, dashboardSel } from "@ducks/dashboard";
import Checkbox from "@wrappers/checkbox";
import Label from "@wrappers/label";
import FacetsList from "./FacetsList";
import { globalOp, globalSel } from "@ducks/global";
import { useParams } from "react-router";
import { routerSel } from "@ducks/router";

export default function FilterBar() {
    const dispatch = useDispatch();
    const {projectId} = useParams();

    const appliedFilters = useSelector(dashboardSel.appliedFiltersSelector);
    const modifiers = useSelector(globalSel.availableDTypesSelector);
    const qs = useSelector(routerSel.routerSearchSelector);

    const typeModifier = modifiers.type;

    useEffect(() => {
        // Reset the filters, due to redirecting
        dispatch(dashboardOp.resetFilters());
        // Setup the filters from query string
        dispatch(dashboardOp.setupFiltersFromQs(qs));
        dispatch(globalOp.fetchAvailableDTypesAsync(projectId));
    }, []);

    const handleFilterSelect = (field: string, val: string) => {
        let value = val !== appliedFilters[field] ? val : '';
        dispatch(dashboardOp.applyFiltersOp({
            [field]: value
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
                                label={opt.label}
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
