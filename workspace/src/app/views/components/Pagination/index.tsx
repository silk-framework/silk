import React from 'react';

export default function Pagination({pagination, onPageChange}) {
    const elements = [];

    let pagesCount = Math.ceil(pagination.total / pagination.limit);
    while (pagesCount--) {
        elements.unshift(pagesCount + 1);
    }

    return (
        <>
            {
                elements.map(i =>
                    i === pagination.current
                        ? <a className={'active'} key={i}>{i} </a>
                        : <a key={i} href="#" onClick={() => onPageChange(i)}>{i} </a>
                )
            }
        </>
    );
}