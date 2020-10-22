import React from "react";

export default function TargetList({ targets, onChange }) {
    const handleSelectTarget = (target) => {
        const arr = targets.map(item => ({
            ...item,
            _selected: target === item.uri
        }));
        onChange(arr);
    };

    return (
        <select onChange={e => handleSelectTarget(e.target.value)}>
            {
                targets.map((target) =>
                    <option
                        key={target.uri}
                        value={target.uri}
                        selected={target._selected}
                    >
                        {target.uri}
                    </option>)
            }
        </select>
    )
}
