import React from 'react';
import PropTypes from 'prop-types';
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

import { ParentStructure } from '../components/ParentStructure';
import RuleTitle from '../elements/RuleTitle';
import ArrowBackButton from '../elements/buttons/ArrowBack';

class MappingHeader extends React.Component {
    static propTypes = {
        rule: PropTypes.object.isRequired,
        showNavigation: PropTypes.bool,
        onRuleIdChange: PropTypes.func,
        onToggleTreeNav: PropTypes.func,
        onToggleDetails: PropTypes.func,
    };
    
    static defaultProps = {
        showNavigation: false,
        onRuleIdChange: () => {},
        onToggleTreeNav: () => {},
        onToggleDetails: () => {},
    };
    
    // jumps to selected rule as new center of view
    handleNavigate = (id, parent, event) => {
        this.props.onRuleIdChange({ newRuleId: id, parentId: parent });
        event.stopPropagation();
    };

    // template rendering
    render() {
        if (_.isEmpty(this.props.rule)) {
            return false;
        }

        const breadcrumbs = _.get(this.props, 'rule.breadcrumbs', []);
        const parent = _.last(breadcrumbs);

        const navBack = _.has(parent, 'id') ? (
            <div className="mdl-card__title-back">
                <ArrowBackButton
                    onNavigate={event => this.handleNavigate(
                        parent.id,
                        this.props.rule.id,
                        event
                    )}
                />
            </div>
        ) : (
            false
        );
        const navBreadcrumbs = (
            <BreadcrumbList>
                {breadcrumbs.map((crumb, idx) => (
                        <BreadcrumbItem
                            key={idx}
                            onClick={event => {
                                this.handleNavigate(
                                    crumb.id,
                                    this.props.rule.id,
                                    event
                                );
                            }}
                            separationChar="/"
                            data-test-selector={'breadcrumb-item'}
                        >
                            <ParentStructure parent={crumb} />
                        </BreadcrumbItem>
                    ))}
                <BreadcrumbItem key={breadcrumbs.length}>
                    <RuleTitle rule={_.get(this.props, 'rule', {})} />
                </BreadcrumbItem>
            </BreadcrumbList>
        );

        return (
            <header className="ecc-silk-mapping__navheader">
                <Card shadow={2}>
                    <CardTitle className="ecc-silk-mapping__navheader-row">
                        {navBack}
                        {navBreadcrumbs}
                    </CardTitle>
                    <CardMenu>
                        <ContextMenu
                            className="ecc-silk-mapping__ruleslistmenu"
                            iconName="tune"
                        >
                            <MenuItem
                                className="ecc-silk-mapping__ruleslistmenu__item-toggletree"
                                onClick={() => this.props.onToggleTreeNav()}
                            >
                                {this.props.showNavigation
                                    ? 'Hide tree navigation'
                                    : 'Show tree navigation'}
                            </MenuItem>
                            <MenuItem
                                className="ecc-silk-mapping__ruleslistmenu__item-expand"
                                onClick={() => {
                                    this.props.onToggleDetails({
                                        expanded: true,
                                    });
                                }}
                            >
                                Expand all
                            </MenuItem>
                            <MenuItem
                                className="ecc-silk-mapping__ruleslistmenu__item-reduce"
                                onClick={() => {
                                    this.props.onToggleDetails({
                                        expanded: false,
                                    });
                                }}
                            >
                                Reduce all
                            </MenuItem>
                        </ContextMenu>
                    </CardMenu>
                </Card>
            </header>
        );
    }
}

export default MappingHeader;
