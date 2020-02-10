import React from "react";

export  default function PageSizer({onChangeSelect, value}) {
    return (
        <select value={value} onChange={(e) => onChangeSelect(e.target.value)}>
            <option value={10}>10</option>
            <option value={25}>25</option>
            <option value={50}>50</option>
            <option value={100}>100</option>
        </select>
    )
}