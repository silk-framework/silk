import React from 'react';
import {RuleTitle, RuleTypes} from './MappingRule/SharedComponents';
import RuleObjectEdit from './MappingRule/ObjectMappingRule';
import _ from 'lodash';
import hierarchicalMappingChannel from '../store';
import {Button, Chip, ConfirmationDialog, DisruptiveButton, DismissiveButton} from 'ecc-gui-elements';
import UseMessageBus from '../UseMessageBusMixin';

const MappingRuleOverviewHeader = React.createClass({
    mixins: [UseMessageBus],
    getInitialState(){
        return {
            expanded: false,
            editing: false,
            askForDiscard: false,
        }
    },
    // jumps to selected rule as new center of view
    handleNavigate(id, event) {
        hierarchicalMappingChannel.subject('ruleId.change').onNext({newRuleId: id, parent: this.props.rule.id});

        event.stopPropagation();
    },
    componentDidMount() {
        this.subscribe(hierarchicalMappingChannel.subject('ruleView.edit'), this.onOpenEdit);
        this.subscribe(hierarchicalMappingChannel.subject('ruleView.closed'), this.onCloseEdit);
    },
    onOpenEdit(obj) {
        console.log('Header', obj, this.props.rule)
        if (this.props.rule.id === obj.id) {
            console.log('open edit for ' + obj.id);
            this.setState({
                editing: true,
            });
        }
        else console.log(obj, this.props.rule);
    },
    onCloseEdit(obj) {
        if (this.props.rule.id === obj.id) {
            console.log('open edit for ' + obj.id);
            this.setState({
                editing: false,
            });
        }
        else console.log(obj, this.props.rule);
    },
    handleDiscardChanges(){
        this.setState({
            expanded: !this.state.expanded,
            askForDiscard: false,
        });
        hierarchicalMappingChannel.subject('ruleView.closed').onNext({id: this.props.rule.id});
    },
    handleCancelDiscard() {
        this.setState({
            askForDiscard: false,
        })
    },
    handleToggleExpand() {
        if (this.state.editing){
            this.setState({
                askForDiscard: true,
            })
        }
        else {
            this.setState({
                expanded: !this.state.expanded,
            })
        }
    },
    render() {

        if (_.isEmpty(this.props.rule)) {
            return false;
        }

        const discardView = this.state.askForDiscard
            ? <ConfirmationDialog
                active={true}
                title="Discard changes"
                confirmButton={
                    <DisruptiveButton disabled={false} onClick={this.handleDiscardChanges}>
                        Continue
                    </DisruptiveButton>
                }
                cancelButton={
                    <DismissiveButton onClick={this.handleCancelDiscard}>
                        Cancel
                    </DismissiveButton>
                }>
                <p>By clicking on CONTINUE, all unsaved changes from the current formular will be destroy.</p>
                <p>Are you sure you want to close the form?</p>
            </ConfirmationDialog>
            : false;


        const breadcrumbs = _.get(this.props, 'rule.breadcrumbs', []);
        const parent = _.last(breadcrumbs);

        let parentTitle = false;
        let backButton = false;

        if (_.has(parent, 'id') && _.has(parent, 'name')) {
            // parentTitle should be the main entity type, or the parent relation if type is not set
            parentTitle = (
                <div className="mdl-card__title-text-sup">
                    <Chip onClick={this.handleNavigate.bind(null, parent.id)}>{parent.name} (fixme)</Chip>
                </div>
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
                    parent={_.get(parent, 'id', '')}
                    parentName={_.get(parent, 'name', '')}
                    edit={false}
                />
            );
        }

        return (
            <div
                className="ecc-silk-mapping__ruleshead"
            >
                {discardView}
                <div className="mdl-card mdl-card--stretch mdl-shadow--2dp">
                    <div className="mdl-card__title mdl-card--border">
                        <div className="mdl-card__title-back">
                            {backButton}
                        </div>
                        <div
                            className="mdl-card__title-text clickable"
                            title={this.state.expand ? "Click to collapse":"Click to expand"}
                            onClick={this.handleToggleExpand}
                        >
                            {parentTitle}
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
