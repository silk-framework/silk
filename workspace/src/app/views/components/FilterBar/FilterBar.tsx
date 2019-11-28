import React, { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { dashboardOp, dashboardSel } from "../../../state/ducks/dashboard";
import Checkbox from "../wrappers/Checkbox/Checkbox";
import Label from "../wrappers/Label/Label";

export default function FilterBar() {
    const dispatch = useDispatch();
    const [selectedType, setSelectedType] = useState({});

    useEffect(() => {
        dispatch(dashboardOp.fetchTypesAsync());
    }, []);

    const handleFilterSelect = (type: string, value: string) => {
        let val = '';
        if (value !== selectedType[type]) {
            val = value
        }
        setSelectedType({
            [type]: val
        });
        dispatch(dashboardOp.setFilterAsync(type, val));
    };

    const modifiers = useSelector(dashboardSel.modifiersSelector);
    console.log(modifiers);
    return (
        <aside>
            {
                modifiers.type &&
                <>
                    <Label>{modifiers.type.label}</Label>
                    {
                        modifiers.type.options.map(opt =>
                            <Checkbox
                                checked={selectedType[modifiers.type.field] === opt.id}
                                label={opt.id}
                                onChange={() => handleFilterSelect(modifiers.type.field, opt.id)}
                                value={opt.id}
                            />
                        )
                    }
                </>
            }
        </aside>
    )
}
