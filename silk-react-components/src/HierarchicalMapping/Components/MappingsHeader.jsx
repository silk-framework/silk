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
    ContextMenu,
    MenuItem,
    BreadcrumbList,
    BreadcrumbItem,
} from '@eccenca/gui-elements';

import Navigation from '../Mixins/Navigation';
import {
    RuleTitle,
    ParentStructure,
} from './MappingRule/SharedComponents';
import hierarchicalMappingChannel from '../store';
import { MESSAGES } from '../constants';


const MappingsHeader = React.createClass({
    // define property types
    propTypes: {
        // currentRuleId: React.PropTypes.string, // selected rule id
    },

    // initilize state
    getInitialState() {
        return {
            showTreenavigation: true,
        };
    },

    componentDidMount() {
        // this.subscribe(
        //     hierarchicalMappingChannel.subject('ruleView.discardAll'),
        //     this.discardAll
        // );
    },
    
    handleToggleRuleDetails(stateExpand) {
        hierarchicalMappingChannel
            .subject(MESSAGES.TOGGLE_DETAILS)
            .onNext(stateExpand);
    },
    
    promoteToggleTreenavigation(stateVisibility) {
        hierarchicalMappingChannel
            .subject(MESSAGES.TREE_NAV.TOGGLE_VISIBILITY)
            .onNext(stateVisibility);
    },
    
    
    handleToggleTreenavigation() {
        this.promoteToggleTreenavigation(!this.state.showTreenavigation);
        this.setState({
            showTreenavigation: !this.state.showTreenavigation,
        });
    },
    // jumps to selected rule as new center of view
    handleNavigate(id, parent, event) {
        hierarchicalMappingChannel
            .subject(MESSAGES.RULE_ID.CHANGE)
            .onNext({newRuleId: id, parentId: parent});
        
        event.stopPropagation();
    },

    // template rendering
    render() {
        if (_.isEmpty(this.props.rule)) {
            return false;
        }

        const breadcrumbs = _.get(this.props, 'rule.breadcrumbs', []);
        const parent = _.last(breadcrumbs);

        const navBack = _.has(parent, 'id') ? (
            <div className="mdl-card__title-back">
                <Button
                    iconName={'arrow_back'}
                    tooltip="Navigate back to parent"
                    onClick={event => {
                        this.handleNavigate(
                            parent.id,
                            this.props.rule.id,
                            event
                        );
                    }}
                />
            </div>
        ) : (
            false
        );

        const self = this;

        const navBreadcrumbs = (
            <BreadcrumbList>
                {breadcrumbs.length > 0
                    ? breadcrumbs.map((crumb, idx) => (
                          <BreadcrumbItem
                              key={idx}
                              onClick={event => {
                                  self.handleNavigate(
                                      crumb.id,
                                      self.props.rule.id,
                                      event
                                  );
                              }}
                              separationChar="/">
                              <ParentStructure parent={crumb} />
                          </BreadcrumbItem>
                      ))
                    : false}
                <BreadcrumbItem key={breadcrumbs.length}>
                    <RuleTitle rule={_.get(this.props, 'rule', {})} />
                </BreadcrumbItem>
            </BreadcrumbList>
        );

        const navMenu = (
            <CardMenu>
                <ContextMenu
                    className="ecc-silk-mapping__ruleslistmenu"
                    iconName="tune">
                    <MenuItem
                        className="ecc-silk-mapping__ruleslistmenu__item-toggletree"
                        onClick={this.handleToggleTreenavigation}>
                        {this.state.showTreenavigation
                            ? 'Hide tree navigation'
                            : 'Show tree navigation'}
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
            </CardMenu>
        );

        return (
            <header className="ecc-silk-mapping__navheader">
                <Card shadow={2}>
                    <CardTitle className="ecc-silk-mapping__navheader-row">
                        {navBack}
                        {navBreadcrumbs}
                    </CardTitle>
                    {navMenu}
                </Card>
            </header>
        );
    },
});

export default MappingsHeader;
