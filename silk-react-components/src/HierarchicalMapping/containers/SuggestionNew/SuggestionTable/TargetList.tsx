import React from "react";

export default function TargetList({ targets, onChange }) {
    const handleSelectTarget = (uri) => {
        onChange(uri);
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
