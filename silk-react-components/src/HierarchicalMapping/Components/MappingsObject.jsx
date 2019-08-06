import React from 'react';
import _ from 'lodash';
import className from 'classnames';
import {
    Button,
    Card,
    CardTitle,
    ConfirmationDialog,
    DisruptiveButton,
    DismissiveButton,
    NotAvailable,
} from '@eccenca/gui-elements';
import { ThingIcon } from './MappingRule/SharedComponents';
import RuleTitle from '../elements/RuleTitle/RuleTitle';
import RuleTypes from '../elements/RuleTypes/RuleTypes';
import ObjectRule from './MappingRule/ObjectMappingRule';
import hierarchicalMappingChannel from '../store';
import UseMessageBus from '../UseMessageBusMixin';
import Navigation from '../Mixins/Navigation';
import { MAPPING_RULE_TYPE_COMPLEX_URI, MAPPING_RULE_TYPE_URI } from '../helpers';
import { MESSAGES } from '../constants';

const MappingsObject = React.createClass({
    mixins: [UseMessageBus],
    getInitialState() {
        return {
            expanded: false,
            editing: false,
            askForDiscard: false,
        };
    },
    componentDidMount() {
        this.subscribe(
            hierarchicalMappingChannel.subject(MESSAGES.RULE_VIEW.TOGGLE),
            ({ expanded, id }) => {
                // only trigger state / render change if necessary
                if (
                    (id === true || id === this.props.rule.id) &&
                    expanded !== this.state.expanded
                ) {
                    this.setState({ expanded });
                }
            }
        );
        this.subscribe(
            hierarchicalMappingChannel.subject(MESSAGES.RULE_VIEW.CHANGE),
            this.onOpenEdit
        );
        this.subscribe(
            hierarchicalMappingChannel.subject(MESSAGES.RULE_VIEW.UNCHANGED),
            this.onCloseEdit
        );
        this.subscribe(
            hierarchicalMappingChannel.subject(MESSAGES.RULE_VIEW.DISCARD_ALL),
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
            .subject(MESSAGES.RULE_VIEW.UNCHANGED)
            .onNext({ id: this.props.rule.id });
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

        const discardView = this.state.askForDiscard ? (
            <ConfirmationDialog
                active
                modal
                title="Discard changes?"
                confirmButton={
                    <DisruptiveButton
                        disabled={false}
                        onClick={this.handleDiscardChanges}
                    >
                        Discard
                    </DisruptiveButton>
                }
                cancelButton={
                    <DismissiveButton onClick={this.handleCancelDiscard}>
                        Cancel
                    </DismissiveButton>
                }
            >
                <p>You currently have unsaved changes.</p>
            </ConfirmationDialog>
        ) : (
            false
        );

        const breadcrumbs = _.get(this.props, 'rule.breadcrumbs', []);
        const parent = _.last(breadcrumbs);

        let content = false;

        if (this.state.expanded) {
            content = (
                <ObjectRule
                    {...this.props.rule}
                    parentId={_.get(parent, 'id', '')}
                    parent={parent}
                    edit={false}
                    handleCopy={this.props.handleCopy}
                    handleClone={this.props.handleClone}
                />
            );
        }

        let uriPattern;

        const uriRuleType = _.get(this.props.rule.rules, 'uriRule.type', false);

        if (uriRuleType === MAPPING_RULE_TYPE_URI) {
            uriPattern = _.get(this, 'props.rule.rules.uriRule.pattern');
        } else if (uriRuleType === MAPPING_RULE_TYPE_COMPLEX_URI) {
            uriPattern = 'URI formula';
        } else {
            uriPattern = (
                <NotAvailable label="automatic default pattern" inline />
            );
        }

        return (
            <div className="ecc-silk-mapping__rulesobject">
                {discardView}
                <Card shadow={0}>
                    <CardTitle>
                        <div className="ecc-silk-mapping__ruleitem">
                            <div
                                className={className(
                                    'ecc-silk-mapping__ruleitem-summary',
                                    {
                                        'ecc-silk-mapping__ruleitem-summary--expanded': this
                                            .state.expanded,
                                    }
                                )}
                            >
                                <div
                                    className="mdl-list__item clickable"
                                    onClick={this.handleToggleExpand}
                                >
                                    <div
                                        className="mdl-list__item-primary-content"
                                    >
                                        <div className="ecc-silk-mapping__ruleitem-headline">
                                            <ThingIcon type="object" />
                                            <RuleTitle
                                                rule={this.props.rule}
                                                className="ecc-silk-mapping__rulesobject__title-property"
                                            />
                                        </div>
                                        <RuleTypes
                                            rule={this.props.rule}
                                            className="ecc-silk-mapping__ruleitem-subline ecc-silk-mapping__rulesobject__title-type"
                                        />
                                        <div className="ecc-silk-mapping__ruleitem-subline ecc-silk-mapping__rulesobject__title-uripattern">
                                            {uriPattern}
                                        </div>
                                    </div>
                                    <div
                                        className="mdl-list__item-secondary-content"
                                        key="action"
                                    >
                                        <Button
                                            className={`silk${this.props.rule.id}`}
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
                            </div>
                        </div>
                    </CardTitle>
                    {content}
                </Card>
            </div>
        );
    },
});

export default MappingsObject;
