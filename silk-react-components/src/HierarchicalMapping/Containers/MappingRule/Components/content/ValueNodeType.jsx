import React from 'react';
import { InfoBox } from '../../../../Components/InfoBox';
import { PropertyTypeLabel } from '../../../../Components/PropertyTypeLabel';
import { PropertyTypeDescription } from '../../../../Components/PropertyTypeDescription';

const propertyTypeLabel = valueType => {
    // Adds optional properties of the property type to the label, e.g. language tag
    if (typeof valueType.lang === 'string') {
        return ` (${valueType.lang})`;
    }
    return '';
};

const ValueNodeType = ({ valueType, nodeType }) => {
    return (
        <div className="ecc-silk-mapping__rulesviewer__propertyType">
            <dl className="ecc-silk-mapping__rulesviewer__attribute">
                <dt className="ecc-silk-mapping__rulesviewer__attribute-label">
                    Data type
                </dt>
                <dd key={nodeType}>
                    <InfoBox>
                        <div
                            className="ecc-silk-mapping__rulesviewer__attribute-title ecc-silk-mapping__rulesviewer__infobox-main">
                            <PropertyTypeLabel
                                name={nodeType}
                                appendedText={propertyTypeLabel(valueType)}
                            />
                        </div>
                        <div
                            className="ecc-silk-mapping__rulesviewer__attribute-info ecc-silk-mapping__rulesviewer__infobox-sub">
                            <PropertyTypeDescription name={nodeType}/>
                        </div>
                    </InfoBox>
                </dd>
            </dl>
        </div>
    )
};

export default ValueNodeType;
