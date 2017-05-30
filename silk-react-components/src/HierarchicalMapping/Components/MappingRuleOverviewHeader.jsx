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
            // FIXME: try to use chip here
            parentTitle = (
                <span onClick={this.handleNavigate.bind(null, parent.id)}>{parent.name}</span>
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
                <RuleObjectEdit
                    {...this.props.rule}
                    edit={false}
                    onClose={this.handleRuleEditClose}
                />
            );
        }

        return (
            <div
                className="ecc-silk-mapping__ruleshead"
            >
                <div className="mdl-card mdl-card--stretch mdl-shadow--2dp">
                    <div className="mdl-card__title mdl-card--border">
                        <div className="mdl-card__title-back">
                            {backButton}
                        </div>
                        <div className="mdl-card__title-text">
                            <div className="mdl-card__title-text-sup">
                                {parentTitle}
                            </div>
                            <div className="mdl-card__title-text-main">
                                <RuleTitle rule={this.props.rule}/>
                            </div>
                            <div className="mdl-card__title-text-sub">
                                <RuleTypes rule={this.props.rule}/>
                            </div>
                        </div>
                        <div className="mdl-card__title-action">
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
