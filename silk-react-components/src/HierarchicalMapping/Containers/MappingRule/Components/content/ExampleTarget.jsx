import React from 'react';
import ExampleView from '../../ExampleView';

const ExampleTarget = ({uriRuleId}) => {
    return (
        <div className="ecc-silk-mapping__rulesviewer__examples">
            <dl className="ecc-silk-mapping__rulesviewer__attribute">
                <dt className="ecc-silk-mapping__rulesviewer__attribute-label">
                    Examples of target data
                </dt>
                <dd>
                    <ExampleView id={uriRuleId}/>
                </dd>
            </dl>
        </div>
    )
};

export default ExampleTarget;
