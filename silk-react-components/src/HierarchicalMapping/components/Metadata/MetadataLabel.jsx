import React from 'react';

const MetadataLabel = ({ label }) => {
    return (
        <div className="ecc-silk-mapping__rulesviewer__label">
            <dl className="ecc-silk-mapping__rulesviewer__attribute">
                <dt className="ecc-silk-mapping__rulesviewer__attribute-label">
                    Label
                </dt>
                <dd className="ecc-silk-mapping__rulesviewer__attribute-info">
                    {label}
                </dd>
            </dl>
        </div>
    )
};

export default MetadataLabel;
