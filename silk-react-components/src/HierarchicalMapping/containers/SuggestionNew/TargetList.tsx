import React, { useState } from "react";

export  default function TargetList({ targets, onChange }) {
    const [selected, setSelected] = useState(targets[0]);

    const handleSelectTarget = (target) => {
        setSelected(target);
        onChange(target);
    };

    return (
        <select>
            {
                targets.map((target) =>
                    <option
                        key={target.uri}
                        value={target.uri}
                        selected={selected.uri === target.uri}
                        onClick={() => handleSelectTarget(target)}
                    >
                        {target.uri}
                    </option>)
            }
        </select>
    )
}
