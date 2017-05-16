
/*
 Whole overview over a hierarchical Mapping on the right, header may be defined here, loops over MappingRule
 */

import React from 'react';
import {UseMessageBus} from 'ecc-mixins';
import MappingRule from './MappingRule';
import {Spinner} from 'ecc-gui-elements';
import hierarchicalMappingChannel from '../store';

const MappingRuleOverview = React.createClass({

    mixins: [UseMessageBus],

    // define property types
    propTypes: {
        //apiBase: React.PropTypes.string.isRequired, // used restApi url
        //project: React.PropTypes.string.isRequired, // used project name
        //transformationTask: React.PropTypes.string, // used transformation
        currentRuleId: React.PropTypes.string, // selected rule id
    },

    // initilize state
    getInitialState() {
        return {
            loading: true,
            ruleData: undefined,
        };
    },

    componentDidMount() {
        hierarchicalMappingChannel.request(
            {
                topic: 'rule.get',
                data: {
                    id: this.props.currentRuleId,
                }
            }
        )
        .subscribe(
            ({rule}) => {
                console.warn('MappingRuleOverview: rule.get', rule);
                this.setState({
                    loading: false,
                    ruleData: rule,
                });
            },
            (err) => {
                console.warn('err MappingRuleOverview: rule.get');
                this.setState({loading: false});
            }
        );
    },

    // template rendering
    render () {
        const loading = this.state.loading ? <Spinner /> : false;

        return (
            <div
                className="ecc-component-hierarchicalMapping__mappingRuleOverview"
            >
                {loading}
                Hello DI. I am MappingRuleOverview.
                <br/>
                <div
                    className="ecc-component-hierarchicalMapping__mappingRuleOverview__header"
                >
                    Selected Rule: {this.props.currentRuleId}
                </div>
                <br/>
                <div
                    className="ecc-component-hierarchicalMapping__mappingRuleOverview__header"
                >
                    ruleData: {JSON.stringify(this.state.ruleData, null, 2)}
                </div>
                <br />
                MappingRule Example: <MappingRule />
            </div>
        );
    },
});

export default MappingRuleOverview;