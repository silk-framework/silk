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
import { ThingIcon } from '../components/ThingIcon';
import RuleTitle from '../elements/RuleTitle';
import RuleTypes from '../elements/RuleTypes';
import ObjectRule from './MappingRule/ObjectRule/ObjectRule';
import { MAPPING_RULE_TYPE_COMPLEX_URI, MESSAGES, MAPPING_RULE_TYPE_URI } from '../utils/constants';
import EventEmitter from '../utils/EventEmitter';
import ExpandButton from '../elements/buttons/ExpandButton';

class RootMappingRule extends React.Component {
    state = {
        expanded: false,
        editing: false,
    };
    
    constructor(props) {
        super(props);
        this.handleRuleToggle = this.handleRuleToggle.bind(this);
        this.onOpenEdit = this.onOpenEdit.bind(this);
        this.onCloseEdit = this.onCloseEdit.bind(this);
        this.handleDiscardChanges = this.handleDiscardChanges.bind(this);
        this.handleToggleExpand = this.handleToggleExpand.bind(this);
        this.discardAll = this.discardAll.bind(this);
    }
    
    componentDidMount() {
        EventEmitter.on(MESSAGES.RULE_VIEW.TOGGLE, this.handleRuleToggle);
        EventEmitter.on(MESSAGES.RULE_VIEW.CHANGE, this.onOpenEdit);
        EventEmitter.on(MESSAGES.RULE_VIEW.UNCHANGED, this.onCloseEdit);
        EventEmitter.on(MESSAGES.RULE_VIEW.DISCARD_ALL, this.discardAll);
    }
    
    componentWillUnmount() {
        EventEmitter.off(MESSAGES.RULE_VIEW.TOGGLE, this.handleRuleToggle);
        EventEmitter.off(MESSAGES.RULE_VIEW.CHANGE, this.onOpenEdit);
        EventEmitter.off(MESSAGES.RULE_VIEW.UNCHANGED, this.onCloseEdit);
        EventEmitter.off(MESSAGES.RULE_VIEW.DISCARD_ALL, this.discardAll);
    }
    
    handleRuleToggle({expanded, id}) {
        // only trigger state / render change if necessary
        if ((id === true || id === this.props.rule.id) && expanded !== this.state.expanded) {
            this.setState({expanded});
        }
    };
    
    onOpenEdit(obj) {
        if (this.props.rule.id === obj.id) {
            this.setState({
                editing: true,
            });
        }
    };
    
    onCloseEdit(obj) {
        if (this.props.rule.id === obj.id) {
            this.setState({
                editing: false,
            });
        }
    };
    
    handleDiscardChanges() {
        this.setState({
            expanded: !this.state.expanded,
        });
        this.props.onAskDiscardChanges(false);
        EventEmitter.emit(MESSAGES.RULE_VIEW.UNCHANGED, {id: this.props.rule.id});
    };
    
    handleToggleExpand() {
        if (this.state.editing) {
            this.props.onAskDiscardChanges(true);
        } else {
            this.setState({
                expanded: !this.state.expanded,
            });
        }
    };
    
    discardAll() {
        this.setState({
            editing: false,
        });
    };
    
    render() {
        if (_.isEmpty(this.props.rule)) {
            return false;
        }
        
        const breadcrumbs = _.get(this.props, 'rule.breadcrumbs', []);
        const parent = _.last(breadcrumbs);
        
        let uriPattern = <NotAvailable label="automatic default pattern" inline/>;
        
        const uriRuleType = _.get(this.props.rule.rules, 'uriRule.type', false);
        if (uriRuleType === MAPPING_RULE_TYPE_URI) {
            uriPattern = _.get(this, 'props.rule.rules.uriRule.pattern');
        } else if (uriRuleType === MAPPING_RULE_TYPE_COMPLEX_URI) {
            uriPattern = 'URI formula';
        }
        
        return (
            <div className="ecc-silk-mapping__rulesobject">
                <Card shadow={0}>
                    <CardTitle>
                        <div className="ecc-silk-mapping__ruleitem">
                            <div
                                className={className(
                                    'ecc-silk-mapping__ruleitem-summary',
                                    {
                                        'ecc-silk-mapping__ruleitem-summary--expanded': this.state.expanded,
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
                                            <ThingIcon type="object"/>
                                            <RuleTitle
                                                rule={this.props.rule}
                                                className="ecc-silk-mapping__rulesobject__title-property"
                                            />
                                        </div>
                                        <RuleTypes
                                            rule={this.props.rule}
                                            className="ecc-silk-mapping__ruleitem-subline ecc-silk-mapping__rulesobject__title-type"
                                        />
                                        <div
                                            className="ecc-silk-mapping__ruleitem-subline ecc-silk-mapping__rulesobject__title-uripattern">
                                            {uriPattern}
                                        </div>
                                    </div>
                                    <div
                                        className="mdl-list__item-secondary-content"
                                        key="action"
                                    >
                                        <ExpandButton
                                            id={this.props.rule.id}
                                            expanded={this.state.expanded}
                                            onExpand={this.handleToggleExpand}
                                        />
                                    </div>
                                </div>
                            </div>
                        </div>
                    </CardTitle>
                    {this.state.expanded && <ObjectRule
                        ruleData={this.props.rule}
                        parentId={_.get(parent, 'id', '')}
                        parent={parent}
                        edit={false}
                        handleCopy={this.props.handleCopy}
                        handleClone={this.props.handleClone}
                        onClickedRemove={this.props.onClickedRemove}
                    />}
                </Card>
            </div>
        );
    }
}

export default RootMappingRule;
