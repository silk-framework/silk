import React, { useState } from "react";

export  default function TargetList({ targets, onChange }) {
    const handleSelectTarget = (target) => {
        const arr = targets.map(item => ({
            ...item,
            _selected: target.uri === item.uri
        }));
        onChange(arr);
    };

    return (
        <select>
            {
                targets.map((target) =>
                    <option
                        key={target.uri}
                        value={target.uri}
                        selected={target._selected}
                        onClick={() => handleSelectTarget(target)}
                    >
                        {target.uri}
                    </option>)
            }
        </select>
    )
}
