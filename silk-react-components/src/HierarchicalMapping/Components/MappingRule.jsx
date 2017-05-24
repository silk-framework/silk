/*
 An individual Mapping Rule Line
*/

import React from 'react';
import {UseMessageBus} from 'ecc-mixins';
import {Button} from 'ecc-gui-elements';
import hierarchicalMappingChannel from '../store';
import RuleValueEdit from './RuleValueEditView';
import RuleObjectEdit from './RuleObjectEditView';

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
        parent: React.PropTypes.bool,
    },

    // initilize state
    getInitialState() {
        // listen for event to expand / collapse mapping rule
        this.subscribe(hierarchicalMappingChannel.subject('rulesView.toggle'), ({expanded}) => {
            // only trigger state / render change if necessary
            if (expanded !== this.state.expanded && !this.props.parent) {
                this.setState({expanded});
            }
        });
        // listen to rule edit event
        this.subscribe(hierarchicalMappingChannel.subject('ruleId.edit'), this.onRuleEdit);

        return {
            expanded: false,
            edit: false,
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
    // show edit view of specific rule id
    onRuleEdit({ruleId}) {
        if (ruleId === this.props.id) {
            this.setState({edit: true});
        }
    },
    handleRuleEditClose(event) {
        this.setState({
            edit: false,
        });
        event.stopPropagation();
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
            parent,
        } = this.props;

        const action = (
            <Button
                iconName={type === 'hierarchical' && !parent  ? 'arrow_nextpage' : (this.state.expanded ? 'expand_less' : 'expand_more')}
                tooltip={type === 'hierarchical' && !parent ? 'Navigate to' : undefined}
                onClick={(event) => {
                    if (type === 'hierarchical' && !parent) {
                        this.handleNavigate();
                    } else {
                        this.handleToggleExpand();
                    }
                    event.stopPropagation();
                }}
            />
        );

        const shortView = (
             <div
                 className="mdl-card__content"
                 onClick={() => {
                     if ((type === 'hierarchical' || type === 'object') && !parent) {
                         return;
                     }
                     this.handleToggleExpand();
                 }}
             >
                 {name}
                 <div>
                    from (todo: get content)
                 </div>
                 <div>
                    by (todo: get content)
                 </div>
                 <div className="action" key="action">{action}</div>
             </div>
            )
        ;

        // FIXME: only show edit / remove buttons for non-hierarchical mappings?
        const expandedView = (
                <div
                    className="mdl-card__content"
                    onClick={() => {
                        if ((type === 'hierarchical' || type === 'object') && !parent) {
                            return;
                        }
                        this.handleToggleExpand();
                    }}
                >
                    <div className="action" key="action">{action}</div>
                    {
                        // FIXME: only temp behaviour until data is correct
                        (type === 'direct' || type === 'complex' ) ? (
                            <RuleValueEdit
                                {...this.props}
                                type="value"
                                edit={this.state.edit}
                                onClose={this.handleRuleEditClose}
                            />
                        ) : (
                            <RuleObjectEdit
                                {...this.props}
                                type="object"
                                edit={this.state.edit}
                                onClose={this.handleRuleEditClose}
                            />
                        )
                    }
                </div>
        );

        return (
            <div
                className="mdl-card mdl-card--stretch mdl-shadow--2dp ecc-component-hierarchicalMapping__mappingRuleOverview__card"
            >
                {this.state.expanded ? expandedView : shortView}
            </div>
        );
    },
});

export default MappingRule;