import React from 'react';
import { ThingName } from './ThingName';
import { ThingDescription } from './ThingDescription';
import { InfoBox } from './InfoBox';
import TargetCardinality from "./TargetCardinality";

const TargetProperty = ({ mappingTargetUri, isObjectMapping, isAttribute = false }) => {
    return (
        <div className="ecc-silk-mapping__rulesviewer__targetProperty">
            <dl className="ecc-silk-mapping__rulesviewer__attribute">
                <dt className="ecc-silk-mapping__rulesviewer__attribute-label">
                    Target property
                </dt>
                <dd>
                    <InfoBox>
                        <div className="ecc-silk-mapping__rulesviewer__attribute-title ecc-silk-mapping__rulesviewer__infobox-main">
                            <ThingName id={mappingTargetUri} />
                        </div>
                        <div className="ecc-silk-mapping__rulesviewer__attribute-info ecc-silk-mapping__rulesviewer__infobox-main">
                            <code>
                                {mappingTargetUri}
                            </code>
                        </div>
                        <div className="ecc-silk-mapping__rulesviewer__attribute-info ecc-silk-mapping__rulesviewer__infobox-sub">
                            <ThingDescription id={mappingTargetUri}/>
                        </div>
                    </InfoBox>
                    <TargetCardinality isAttribute={isAttribute} isObjectMapping={isObjectMapping} editable={false}/>
                </dd>
            </dl>
        </div>
    )
};

export default TargetProperty;
