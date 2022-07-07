import React from 'react';

const ObjectSourcePath = ({ children }) => {
    return (
        <div className="ecc-silk-mapping__rulesviewer__sourcePath">
            <dl className="ecc-silk-mapping__rulesviewer__attribute">
                <dt className="ecc-silk-mapping__rulesviewer__attribute-label">
                    Value path
                </dt>
                <dd className="ecc-silk-mapping__rulesviewer__attribute-info">
                    {children}
                </dd>
            </dl>
        </div>
    )
};

export default ObjectSourcePath;
