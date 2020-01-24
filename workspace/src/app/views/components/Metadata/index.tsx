import React from 'react';
import './Metadata.scss';
import Card from "@wrappers/card";
import { H4 } from "@wrappers/typography";

export default function ({ metadata }) {
    const { name, description } = metadata;
    return (
        <>
            <div className='metadata-block'>
                <Card>
                    <H4>
                        Details & Metadata
                    </H4>
                    <p>
                        Name: {name}
                    </p>
                    {
                        description && <p>Description: {description}</p>
                    }
                </Card>
            </div>
        </>
    );
}
