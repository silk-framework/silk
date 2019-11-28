import React from 'react';

export default function Pagination({ pagination, onPageChange }) {
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
                        ? <span>{i} </span>
                        : <a href="#" onClick={() => onPageChange(i)}>{i} </a>
                )
            }
        </>
    );
}
