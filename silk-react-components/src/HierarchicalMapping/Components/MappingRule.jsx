/*
 An individual Mapping Rule Line
*/

import React from 'react';
import {UseMessageBus} from 'ecc-mixins';
import {Button} from 'ecc-gui-elements';
import hierarchicalMappingChannel from '../store';

const MappingRule = React.createClass({

    mixins: [UseMessageBus],

    // define property types
    propTypes: {
        comment: React.PropTypes.string,
        id: React.PropTypes.string,
        name: React.PropTypes.string,
        operator: React.PropTypes.object,
        type: React.PropTypes.string, // mapping type
        typeRules: React.PropTypes.array,
        mappingTarget: React.PropTypes.object,
        sourcePath: React.PropTypes.string,
        targetProperty: React.PropTypes.string,
        pattern: React.PropTypes.string,
        uriRule: React.PropTypes.object,
    },

    // initilize state
    getInitialState() {
        return {
            expanded: false,
        };
    },
    // jumps to selected rule as new center of view
    handleNavigate() {
        hierarchicalMappingChannel.subject('ruleId.change').onNext({newRuleId: this.props.id});
    },
    // show / hide additional row details
    handleToggleExpand() {
        this.setState({expanded: !this.state.expanded});
    },
    // open edit view
    handleEdit() {
        hierarchicalMappingChannel.subject('ruleId.edit').onNext({rule: this.props});
    },

    // template rendering
    render () {
        const {
            comment,
            id,
            name,
            operator,
            type,
            typeRules,
            mappingTarget = {},
            sourcePath,
            targetProperty,
            pattern,
            uriRule,
        } = this.props;

        const action = (
            <Button
                iconName={type === 'hierarchical' ? 'arrow_nextpage' : (this.state.expanded ? 'expand_less' : 'expand_more')}
                tooltip={type === 'hierarchical' ? 'Navigate to' : undefined}
                onClick={(event) => {
                    if (type === 'hierarchical') {
                        this.handleNavigate();
                    } else {
                        console.log('debug onClick action:', this.props);
                    }
                    event.stopPropagation();
                }}
            />
        );

        const shortView = (
             <tr
                 className={this.state.expanded ? 'is-extended' : ''}
                 onClick={this.handleToggleExpand}
             >
                 <td key="ruleType">{_.upperFirst(type)} mapping</td>
                 <td key="source">{sourcePath}</td>
                 <td key="targetProperty">{mappingTarget.URI}</td>
                 <td key="targetType">{_.get(mappingTarget, 'valueType.nodeType')}</td>
                 <td className="action" key="action">{action}</td>
             </tr>
            )
        ;

        // FIXME: only show edit / remove buttons for non-hierarchical mappings?
        const expandedView = (
            this.state.expanded ? (
                <tr
                    className="ecc-component-hierarchicalMapping__mappingRuleOverview__card__details"
                >
                    <td colSpan="5">
                        <div>
                            ID: {id}
                            <br/>
                            Comment: {comment}
                        </div>
                        <div
                            className="ecc-component-hierarchicalMapping__mappingRuleOverview__card__details__actionrow"
                        >
                            <Button
                                className="ecc-component-hierarchicalMapping__mappingRuleOverview__card__details__actionrow-edit"
                                onClick={this.handleEdit}
                            >
                                Edit
                            </Button>
                            <Button
                                className="ecc-component-hierarchicalMapping__mappingRuleOverview__card__details__actionrow-remove"
                                onClick={() => {}}
                                disabled
                            >
                                Remove
                            </Button>
                        </div>
                    </td>
                </tr>
            ) : false
        );

        return (
            <tbody
                className="ecc-component-hierarchicalMapping__mappingRuleOverview__card"
            >
                {shortView}
                {expandedView}
            </tbody>
        );
    },
});

export default MappingRule;