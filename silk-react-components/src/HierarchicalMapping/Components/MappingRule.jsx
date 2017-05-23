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
        // listen for event to expand / collapse mapping rule
        this.subscribe(hierarchicalMappingChannel.subject('rulesView.toggle'), ({expanded}) => {
            this.setState({expanded});
        });

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

        console.log('debug this.state.ruleData', this.props);

        const action = (
            <Button
                iconName={type === 'hierarchical' ? 'arrow_nextpage' : (this.state.expanded ? 'expand_less' : 'expand_more')}
                tooltip={type === 'hierarchical' ? 'Navigate to' : undefined}
                onClick={(event) => {
                    if (type === 'hierarchical') {
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
                 //className={this.state.expanded ? 'is-extended' : ''}
                 onClick={this.handleToggleExpand}
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
                    //className="ecc-component-hierarchicalMapping__mappingRuleOverview__card__details"
                    className="mdl-card__content"
                    onClick={this.handleToggleExpand}
                >
                    <div>
                        <h5>Target property</h5> {targetProperty}
                    </div>
                    <div>
                        <h5>Source property</h5>
                        {type} mapping from (todo: get content)
                    </div>
                        {comment ? ( <div> <h5>Comment</h5>{comment}</div>) : false}
                    <div>
                        by (todo: get content)
                    </div>
                    <div>
                        on (todo: get content)
                    </div>
                    <div className="action" key="action">{action}</div>
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