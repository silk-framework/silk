import React from 'react';

export function Pagination({pagination, onPageChange}) {
    const elements = [];

    let pagesCount = Math.ceil(pagination.total / pagination.limit);
    while (pagesCount--) {
        elements.unshift(pagesCount + 1);
    }

    return (
        <>
            Pages:
            {
                elements.map(i =>
                    i === pagination.current
                        ? <span key={i}>{i} </span>
                        : <a key={i} href="#" onClick={() => onPageChange(i)}>{i} </a>
                )
            }
        </>
    );
}

export function Selection({onChangeSelect, value}) {
    return (
            <select value={value} onChange={(e) => onChangeSelect(e.target.value)}>
                <option value={10}>10</option>
                <option value={25}>25</option>
                <option value={50}>50</option>
                <option value={100}>100</option>
            </select>
    )
}
