import React from 'react';
import { Button } from "@wrappers/index";

export function EmptyFileWidget({ onFileAdd }) {
    return (
        <div>
            <p>No files are found, add them here to use it later</p>
            <Button kind={'primary'} onClick={onFileAdd}>+ Add File</Button>
        </div>
    )
}
