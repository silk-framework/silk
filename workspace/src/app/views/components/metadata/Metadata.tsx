import React from 'react';
import './Metadata.scss';

export default function Metadata({ metadata }) {
    const { name, description } = metadata;
    return (
        <>
            <div className='metadata-block'>
                <h4 className='title'>Details & Metadata</h4>
                <div className="content">
                    <p>
                        Name: {name}
                    </p>
                    {
                        description && <p>Description: {description}</p>
                    }
                </div>
            </div>
        </>
    );
}
