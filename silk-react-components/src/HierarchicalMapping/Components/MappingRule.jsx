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

    handleNavigate(ruleId) {
        hierarchicalMappingChannel.subject('ruleId.change').onNext({newRuleId: ruleId});
    },
    handleToggleExpand() {
        this.setState({expanded: !this.state.expanded});
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
                iconName="arrow_nextpage"
                tooltip="Jump to"
                /*iconName="expand_more"
                 iconName="arrow_nextpage"
                 iconName="arrow_prevpage"
                 iconName="arrow_lastpage"
                 iconName="arrow_firstpage"
                 iconName="arrow_dropdown"
                 */
                onClick={(event) => {
                    console.log('debug onClick action:', this.props);
                    /*this.handleNavigate(id)*/
                    event.stopPropagation();
                }}
            />
        );

        const shortView = [
            <td key="ruleType">{_.upperFirst(type)} mapping</td>,
            <td key="source">{sourcePath}</td>,
            <td key="targetProperty">{mappingTarget.URI}</td>,
            <td key="targetType">{_.get(mappingTarget, 'valueType.nodeType')}</td>,
            <td key="action">{action}</td>,
        ];

        const expandedView = (
            this.state.expanded ? (
                <tr colSpan="5">
                    <td>
                        <div>
                            ID: {id}
                            <br/>
                            Comment: {comment}
                        </div>
                    </td>
                </tr>
            ) : false
        );

        return (
            <tbody
                className="ecc-component-hierarchicalMapping__mappingRuleOverview__card"
            >
                <tr
                    className="ecc-component-hierarchicalMapping__mappingRuleOverview__card__short"
                    onClick={this.handleToggleExpand}
                >
                    {shortView}
                </tr>
                {expandedView}
            </tbody>
        )
    },
});

export default MappingRule;