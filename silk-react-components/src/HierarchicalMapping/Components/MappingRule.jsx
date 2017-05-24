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
    handleToggleExpand({force = false}) {
        if (!this.state.edit || force) {
            this.setState({expanded: !this.state.expanded});
        }
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
            id,
            type,
            parent,
        } = this.props;

        const action = (
            <Button
                iconName={type === 'object' && !parent  ? 'arrow_nextpage' : (this.state.expanded ? 'expand_less' : 'expand_more')}
                tooltip={type === 'object' && !parent ? 'Navigate to' : undefined}
                onClick={(event) => {
                    if (type === 'object' && !parent) {
                        this.handleNavigate();
                    } else {
                        this.handleToggleExpand({force: true});
                    }
                    event.stopPropagation();
                }}
            />
        );

        const shortView = (
             <div
                 className="mdl-card__content"
                 onClick={() => {
                     if (type === 'object' && !parent) {
                         return;
                     }
                     this.handleToggleExpand({});
                 }}
             >
                 {id}
                 <div>
                    from (todo: get content after store implementation)
                 </div>
                 <div>
                    by (todo: get content after store implementation)
                 </div>
                 <div className="action" key="action">{action}</div>
             </div>
            )
        ;

        // FIXME: only show edit / remove buttons for non-object mappings?
        const expandedView = (
                <div
                    className="mdl-card__content"
                    onClick={() => {
                        if ((type === 'object' || type === 'root') && !parent) {
                            return;
                        }
                        this.handleToggleExpand({});
                    }}
                >
                    <div className="action" key="action">{action}</div>
                    {
                        (type === 'object' || type === 'root') ? (
                            <RuleObjectEdit
                                {...this.props}
                                type="object"
                                edit={this.state.edit}
                                onClose={this.handleRuleEditClose}
                            />
                        ) : (
                            <RuleValueEdit
                                {...this.props}
                                type="value"
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