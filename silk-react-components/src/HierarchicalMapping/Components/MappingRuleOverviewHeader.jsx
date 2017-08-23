import React from 'react';
import {
    RuleTitle,
    RuleTypes,
    ParentElement,
} from './MappingRule/SharedComponents';
import RuleObjectEdit from './MappingRule/ObjectMappingRule';
import _ from 'lodash';
import hierarchicalMappingChannel from '../store';
import {
    Button,
    Chip,
    ConfirmationDialog,
    DisruptiveButton,
    DismissiveButton,
} from 'ecc-gui-elements';
import UseMessageBus from '../UseMessageBusMixin';

const MappingRuleOverviewHeader = React.createClass({
    mixins: [UseMessageBus],
    getInitialState() {
        return {
            expanded: false,
            editing: false,
            askForDiscard: false,
        };
    },
    // jumps to selected rule as new center of view
    handleNavigate(id, event) {
        hierarchicalMappingChannel
            .subject('ruleId.change')
            .onNext({newRuleId: id, parent: this.props.rule.id});

        event.stopPropagation();
    },
    componentDidMount() {
        this.subscribe(
            hierarchicalMappingChannel.subject('ruleView.change'),
            this.onOpenEdit
        );
        this.subscribe(
            hierarchicalMappingChannel.subject('ruleView.unchanged'),
            this.onCloseEdit
        );
        this.subscribe(
            hierarchicalMappingChannel.subject('ruleView.discardAll'),
            this.discardAll
        );
    },
    onOpenEdit(obj) {
        if (this.props.rule.id === obj.id) {
            this.setState({
                editing: true,
            });
        }
    },
    onCloseEdit(obj) {
        if (this.props.rule.id === obj.id) {
            this.setState({
                editing: false,
            });
        }
    },
    handleDiscardChanges() {
        this.setState({
            expanded: !this.state.expanded,
            askForDiscard: false,
        });
        hierarchicalMappingChannel
            .subject('ruleView.unchanged')
            .onNext({id: this.props.rule.id});
    },
    handleCancelDiscard() {
        this.setState({
            askForDiscard: false,
        });
    },
    handleToggleExpand() {
        if (this.state.editing) {
            this.setState({
                askForDiscard: true,
            });
        } else {
            this.setState({
                expanded: !this.state.expanded,
            });
        }
    },
    discardAll() {
        this.setState({
            editing: false,
        });
    },
    render() {
        if (_.isEmpty(this.props.rule)) {
            return false;
        }

        const discardView = this.state.askForDiscard
            ? <ConfirmationDialog
                  active
                  modal
                  title="Discard changes?"
                  confirmButton={
                      <DisruptiveButton
                          disabled={false}
                          onClick={this.handleDiscardChanges}>
                          Discard
                      </DisruptiveButton>
                  }
                  cancelButton={
                      <DismissiveButton onClick={this.handleCancelDiscard}>
                          Cancel
                      </DismissiveButton>
                  }>
                  <p>You currently have unsaved changes.</p>
              </ConfirmationDialog>
            : false;

        const breadcrumbs = _.get(this.props, 'rule.breadcrumbs', []);
        const parent = _.last(breadcrumbs);

        let parentTitle = false;
        let backButton = false;

        if (_.has(parent, 'id')) {
            parentTitle = (
                <div className="mdl-card__title-text-sup">
                    <Chip onClick={this.handleNavigate.bind(null, parent.id)}>
                        <ParentElement parent={parent} />
                    </Chip>
                </div>
            );

            backButton = (
                <Button
                    iconName={'chevron_left'}
                    tooltip="Navigate back to parent"
                    onClick={this.handleNavigate.bind(null, parent.id)}
                />
            );
        }

        let content = false;

        if (this.state.expanded) {
            content = (
                <RuleObjectEdit
                    {...this.props.rule}
                    parentId={_.get(parent, 'id', '')}
                    parent={parent}
                    edit={false}
                />
            );
        }

        return (
            <div className="ecc-silk-mapping__ruleshead">
                {discardView}
                <div className="mdl-card mdl-card--stretch">
                    <div className="mdl-card__title mdl-card--border">
                        <div className="mdl-card__title-back">
                            {backButton}
                        </div>
                        <div
                            className="mdl-card__title-text clickable"
                            onClick={this.handleToggleExpand}>
                            {parentTitle}
                            <div className="mdl-card__title-text-main">
                                <RuleTitle rule={this.props.rule} />
                            </div>
                            <div className="mdl-card__title-text-sub">
                                <RuleTypes rule={this.props.rule} />
                            </div>
                        </div>
                        <div className="mdl-card__title-action">
                            <Button
                                iconName={
                                    this.state.expanded
                                        ? 'expand_less'
                                        : 'expand_more'
                                }
                                onClick={ev => {
                                    this.handleToggleExpand();
                                }}
                            />
                        </div>
                    </div>
                    {content}
                </div>
            </div>
        );
    },
});

export default MappingRuleOverviewHeader;
