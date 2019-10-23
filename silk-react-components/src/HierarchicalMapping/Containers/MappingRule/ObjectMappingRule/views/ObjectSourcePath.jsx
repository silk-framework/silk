import React from 'react';
import { SourcePath } from '../../../../Components/SourcePath';

const ObjectSourcePath = ({ type, sourcePath}) => {
    return (
        <div className="ecc-silk-mapping__rulesviewer__sourcePath">
            <dl className="ecc-silk-mapping__rulesviewer__attribute">
                <dt className="ecc-silk-mapping__rulesviewer__attribute-label">
                    Value path
                </dt>
                <dd className="ecc-silk-mapping__rulesviewer__attribute-info">
                    <SourcePath
                        rule={{
                            type,
                            sourcePath
                        }}
                    />
                </dd>
            </dl>
        </div>
    )
};

export default ObjectSourcePath;
