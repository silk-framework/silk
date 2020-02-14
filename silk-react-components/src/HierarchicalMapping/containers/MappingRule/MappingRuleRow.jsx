import { ThingIcon } from '../../components/ThingIcon';
import _ from 'lodash';
import { SourcePath } from '../../components/SourcePath';
import React from 'react';
import RuleTypes from '../../elements/RuleTypes';
import { getRuleLabel } from '../../utils/getRuleLabel';

class MappingRuleRow extends React.Component {
    render() {
        const {mappingTarget, metadata, rules, sourcePath, type} = this.props;
        
        const label = _.get(metadata, 'label', '');
        const ruleLabelData = getRuleLabel({label, uri: mappingTarget.uri});
        const statusType = _.get(this.props, 'status[0].type', false);
        const statusMsg = _.get(this.props, 'status[0].message', false);
        return (
            <div className="mdl-list__item-primary-content">
                
                <div className="ecc-silk-mapping__ruleitem-headline ecc-silk-mapping__ruleitem-info-targetstructure">
                    <ThingIcon
                        type={type}
                        status={statusType}
                        message={statusMsg}
                    />
                    <div className="ecc-silk-mapping__ruleitem-label">
                        {ruleLabelData.displayLabel}
                    </div>
                    {ruleLabelData.uri && <div
                        className="ecc-silk-mapping__ruleitem-extraline ecc-silk-mapping__ruleitem-url">{ruleLabelData.uri}</div>}
                </div>
                <div className="ecc-silk-mapping__ruleitem-subline ecc-silk-mapping__ruleitem-info-editinfo">
                    <span className="hide-in-table">DataType:</span>{' '}
                    <RuleTypes
                        rule={{
                            type,
                            mappingTarget,
                            rules,
                        }}
                    />
                </div>
                <div className="ecc-silk-mapping__ruleitem-subline ecc-silk-mapping__ruleitem-info-sourcestructure">
                    <span className="hide-in-table">from</span>{' '}
                    <SourcePath
                        rule={{
                            type,
                            sourcePath,
                        }}
                    />
                </div>
            </div>
        )
    }
}

export default MappingRuleRow;
