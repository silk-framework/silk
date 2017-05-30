import React from 'react';
import {RuleTitle, RuleTypes} from './RuleComponents';
import RuleObjectEdit from './RuleObjectEditView';
import _ from 'lodash';
import hierarchicalMappingChannel from '../store';
import {Button} from 'ecc-gui-elements';

const MappingRuleOverviewHeader = React.createClass({
    getInitialState(){
        return {
            expanded: false,
        }
    },
    // jumps to selected rule as new center of view
    handleNavigate(id, event) {
        hierarchicalMappingChannel.subject('ruleId.change').onNext({newRuleId: id});

        event.stopPropagation();
    },
    handleToggleExpand() {
        this.setState({
            expanded: !this.state.expanded,
        })
    },
    render() {

        if (_.isEmpty(this.props.rule)) {
            return false;
        }

        const breadcrumbs = _.get(this.props, 'rule.breadcrumbs', []);
        const parent = _.last(breadcrumbs);

        let parentTitle = false;
        let backButton = false;

        if (_.has(parent, 'id') && _.has(parent, 'name')) {
            parentTitle = (
                <small>
                    <a onClick={this.handleNavigate.bind(null, parent.id)}>{parent.name}</a><br/>
                </small>
            );
            backButton = (
                <Button
                    iconName={'arrow_back'}
                    onClick={this.handleNavigate.bind(null, parent.id)}
                />
            )
        }

        let content = false;

        if (this.state.expanded) {
            content = (
                <div className="mdl-card__content">
                    <RuleObjectEdit
                        {...this.props.rule}
                        edit={false}
                        onClose={this.handleRuleEditClose}
                    />
                </div>
            );
        }

        return (
            <div
                className="ecc-silk-mapping__ruleshead"
            >
                <div className="mdl-card mdl-card--stretch mdl-shadow--2dp">
                    <div className="mdl-card__title">
                        <div>
                            {backButton}
                        </div>
                        <div>
                            {parentTitle}
                            <b><RuleTitle rule={this.props.rule}/></b><br/>
                            <small><RuleTypes rule={this.props.rule}/></small>
                        </div>
                        <div>
                            <Button
                                iconName={this.state.expanded ? 'expand_less' : 'expand_more'}
                                onClick={(ev) => {
                                    this.handleToggleExpand();
                                }}
                            />
                        </div>
                    </div>
                    {content}
                </div>
            </div>
        )
    }
});

export default MappingRuleOverviewHeader;
