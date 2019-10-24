import React from 'react';
import getUriOperatorsRecursive from '../../../../utils/getUriOperators';

const ValueSourcePaths = ({ paths, operator, children }) => {
    const operators = getUriOperatorsRecursive(operator, []);
    return (
        <div className="ecc-silk-mapping__rulesviewer__sourcePath">
            <dl className="ecc-silk-mapping__rulesviewer__attribute">
                <dt className="ecc-silk-mapping__rulesviewer__attribute-label">
                    Value formula
                </dt>
                <dd className="ecc-silk-mapping__rulesviewer__attribute-info">
                    Formula uses {paths.length} value path{paths.length >
                1
                    ? 's'
                    : ''}:&nbsp;
                    <code>{paths.join(', ')}</code>
                    &nbsp;and {operators.length} operator
                    function{operators.length > 1
                    ? 's'
                    : ''}:&nbsp;
                    <code>{operators.join(', ')}</code>.
                    {children}
                </dd>
            </dl>
        </div>
    );
};

export default ValueSourcePaths;
