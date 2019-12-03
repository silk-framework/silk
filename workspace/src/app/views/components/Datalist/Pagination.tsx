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
                        ? <span key={i}>{i} </span>
                        : <a key={i} href="#" onClick={() => onPageChange(i)}>{i} </a>
                )
            }
        </>
    );
}
