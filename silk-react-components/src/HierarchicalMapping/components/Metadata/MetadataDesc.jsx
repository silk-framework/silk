import React from 'react';

const MetadataDesc = ({ description }) => {
    return (
        <div className="ecc-silk-mapping__rulesviewer__comment">
            <dl className="ecc-silk-mapping__rulesviewer__attribute">
                <dt className="ecc-silk-mapping__rulesviewer__attribute-label">
                    Description
                </dt>
                <dd className="ecc-silk-mapping__rulesviewer__attribute-info">
                    {description}
                </dd>
            </dl>
        </div>
    )
};

export default MetadataDesc;
