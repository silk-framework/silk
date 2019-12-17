import React from 'react';
import TagHeader from "./TagHeader";

interface IProps {
    children: any;
    label?: string
}

const styles = {
    'marginLeft': '-10px'
};

export default function TagsGroup({ children, label }: IProps) {
    return (
        <div style={styles}>
            {label && <TagHeader label={label}/>}
            {children}
        </div>
    )
}
