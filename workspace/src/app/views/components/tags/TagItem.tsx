import React from "react";
import Tag from "@wrappers/tag";

const styles = {
    display: 'inline-flex',
    alignItems: 'center',
    borderRadius: '0px',
    backgroundColor: '#5c7080',
    minWidth: '20px',
    maxWidth: '100%',
    minHeight: '20px',
    padding: '2px 6px',
    lineHeight: '16px',
    color: '#f5f8fa',
    fontSize: '12px',
};

export default function TagItem({label, onFacetRemove}) {
    return (
        <Tag
            style={styles}
            onRemove={onFacetRemove}
        >
            {label}
        </Tag>
    )
}
