import React from 'react';
import { ThingName } from '../../../../Components/ThingName';
import { ThingDescription } from '../../../../Components/ThingDescription';
import { InfoBox } from '../../../../Components/InfoBox';

const ObjectTargetProperty = ({ mappingTargetUri }) => {
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
                </dd>
            </dl>
        </div>
    )
};

export default ObjectTargetProperty;
