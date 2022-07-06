import React, {Component} from 'react';
import {NotAvailable} from 'gui-elements-deprecated';
import {MAPPING_RULE_TYPE_COMPLEX_URI, MAPPING_RULE_TYPE_URI} from '../../utils/constants';
import getPathsRecursive from '../../utils/getUriPaths';
import getUriOperatorsRecursive from '../../utils/getUriOperators';
import ComplexEditButton from '../../elements/buttons/ComplexEditButton';
import ComplexDeleteButton from '../../elements/buttons/ComplexeDeleteButton';

class ObjectUriPattern extends Component {
    render() {
        const {uriRule} = this.props;
        const {type, pattern} = uriRule;

        let uriPattern = <NotAvailable label="automatic default pattern" inline/>;

        let uriPatternLabel = 'URI pattern';
        let tooltipText = 'Create URI formula';

        let removeButton = <ComplexDeleteButton
            onDelete={this.props.onRemoveUriRule}
        />;

        if (type === MAPPING_RULE_TYPE_URI) {
            uriPattern = <code>{pattern}</code>;
            tooltipText = 'Convert URI pattern to URI formula';
        } else if (type === MAPPING_RULE_TYPE_COMPLEX_URI) {
            const paths = getPathsRecursive(uriRule.operator);
            const operators = getUriOperatorsRecursive(uriRule.operator);

            tooltipText = 'Edit URI formula';
            uriPatternLabel = 'URI formula';

            uriPattern = (
                <span>
                    URI uses {paths.length} value{' '}
                    {paths.length > 1 ? 'paths' : 'path'}:&nbsp;
                    <code>{paths.join(', ')}</code>&nbsp;and {operators.length}&nbsp;
                    operator {operators.length > 1 ? 'functions' : 'function'}:&nbsp;<code>
                        {operators.join(', ')}
                    </code>.
                </span>
            );
        } else {
            removeButton = false;
        }

        return (
            <div className="ecc-silk-mapping__rulesviewer__idpattern">
                <div className="ecc-silk-mapping__rulesviewer__comment">
                    <dl className="ecc-silk-mapping__rulesviewer__attribute">
                        <dt className="ecc-silk-mapping__rulesviewer__attribute-label">
                            {uriPatternLabel}
                        </dt>
                        <dd className="ecc-silk-mapping__rulesviewer__attribute-info">
                            {uriPattern}
                            <ComplexEditButton
                                onClick={this.props.onEditUriRule}
                                tooltip={tooltipText}
                            />
                            {removeButton}
                        </dd>
                    </dl>
                </div>
            </div>
        );
    }
}

export default ObjectUriPattern;
