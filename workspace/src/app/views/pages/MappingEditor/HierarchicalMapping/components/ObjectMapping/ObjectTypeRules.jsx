import React from 'react';
import { ThingName } from '../ThingName';
import { ThingDescription } from '../ThingDescription';
import { InfoBox } from '../InfoBox';

const ObjectTypeRules = ({ typeRules }) => {
    return (
        <div className="ecc-silk-mapping__rulesviewer__targetEntityType">
            <dl className="ecc-silk-mapping__rulesviewer__attribute">
                <dt className="ecc-silk-mapping__rulesviewer__attribute-label">
                    {typeRules.length > 1
                        ? 'Target entity types'
                        : 'Target entity type'}
                </dt>
                {typeRules.map((typeRule, idx) => (
                    <dd key={`TargetEntityType_${idx}`}>
                        <InfoBox>
                            <div className="ecc-silk-mapping__rulesviewer__attribute-title ecc-silk-mapping__rulesviewer__infobox-main">
                                <ThingName
                                    id={
                                        typeRule.typeUri
                                    }
                                />
                            </div>
                            <div className="ecc-silk-mapping__rulesviewer__attribute-info ecc-silk-mapping__rulesviewer__infobox-main">
                                <code>
                                    {typeRule.typeUri}
                                </code>
                            </div>
                            <div className="ecc-silk-mapping__rulesviewer__attribute-info ecc-silk-mapping__rulesviewer__infobox-sub">
                                <ThingDescription
                                    id={
                                        typeRule.typeUri
                                    }
                                />
                            </div>
                        </InfoBox>
                    </dd>
                ))}
            </dl>
        </div>
    )
};

export default ObjectTypeRules;
