/*
    provides a sticky header that holds all necessary functions
*/

import React from 'react';
import _ from 'lodash';

import {
    Button,
    Card,
    CardTitle,
    CardMenu,
    Chip,
    ContextMenu,
    MenuItem,
} from 'ecc-gui-elements';

import Navigation from '../Mixins/Navigation';
import {
    RuleTitle,
    RuleTypes,
    ParentElement,
} from './MappingRule/SharedComponents';
import {MAPPING_RULE_TYPE_DIRECT, MAPPING_RULE_TYPE_OBJECT} from '../helpers';

const MappingsHeader = React.createClass({
    mixins: [Navigation],

    // define property types
    propTypes: {
        //currentRuleId: React.PropTypes.string, // selected rule id
    },

    // initilize state
    getInitialState() {
        return {
        };
    },

    componentDidMount() {
        // this.subscribe(
        //     hierarchicalMappingChannel.subject('ruleView.discardAll'),
        //     this.discardAll
        // );
    },

    // template rendering
    render() {
        if (_.isEmpty(this.props.rule)) {
            return false;
        }

        const breadcrumbs = _.get(this.props, 'rule.breadcrumbs', []);
        const parent = _.last(breadcrumbs);

        const navBack = _.has(parent, 'id') ?
            <div className="mdl-card__title-back">
                <Button
                    iconName={'chevron_left'}
                    tooltip="Navigate back to parent"
                    onClick={this.handleNavigate.bind(null, parent.id)}
                />
            </div> : false;

        /*
            - check for parent
            - if there is no parent, the navBreadcrumbs = false
            - if there is only one parent, then put it in chip, clickable
            - TODO: if there are more than one parent then create menu with tree and put ellipsis (or horizontal_more) in chip as toggler
        */
        const navBreadcrumbs = _.has(parent, 'id') ? (
            <div className="mdl-card__title-text-breadcrumbs">
                <Chip onClick={this.handleNavigate.bind(null, parent.id)}>
                    <ParentElement parent={parent} />
                </Chip>
            </div>
        ) : false

        const navTitle = <div className="mdl-card__title-text">
            <div className="mdl-card__title-text-main">
                <RuleTitle rule={_.get(this.props, 'rule', {})} />
            </div>
            <div className="mdl-card__title-text-sub">
                <RuleTypes rule={_.get(this.props, 'rule', {})} />
            </div>
        </div>;

        const navMenu = <div className="mdl-card__title-action">
            <ContextMenu className="ecc-silk-mapping__ruleslistmenu">
                <MenuItem
                    className="ecc-silk-mapping__ruleslistmenu__item-add-value"
                    onClick={() => {
                        this.handleCreate({type: MAPPING_RULE_TYPE_DIRECT});
                    }}>
                    Add value mapping
                </MenuItem>
                <MenuItem
                    className="ecc-silk-mapping__ruleslistmenu__item-add-object"
                    onClick={() => {
                        this.handleCreate({type: MAPPING_RULE_TYPE_OBJECT});
                    }}>
                    Add object mapping
                </MenuItem>
                <MenuItem
                    className="ecc-silk-mapping__ruleslistmenu__item-autosuggest"
                    onClick={this.handleShowSuggestions}>
                    Suggest mappings
                </MenuItem>
                <MenuItem
                    className="ecc-silk-mapping__ruleslistmenu__item-expand"
                    onClick={() => {
                        this.handleToggleRuleDetails({
                            expanded: true,
                        });
                    }}>
                    Expand all
                </MenuItem>
                <MenuItem
                    className="ecc-silk-mapping__ruleslistmenu__item-reduce"
                    onClick={() => {
                        this.handleToggleRuleDetails({
                            expanded: false,
                        });
                    }}>
                    Reduce all
                </MenuItem>
            </ContextMenu>
        </div>;

        return (
            <header className="ecc-silk-mapping__navheader">
                <Card shadow={2}>
                    <CardTitle className="ecc-silk-mapping__navheader-row">
                        {navBack}
                        {navBreadcrumbs}
                        {navTitle}
                        {navMenu}
                    </CardTitle>
                </Card>
            </header>
        );
    },
});

export default MappingsHeader;
