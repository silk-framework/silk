/*
 An individual Mapping Rule Line
*/

import React from 'react';
import {UseMessageBus} from 'ecc-mixins';
import {Button} from 'ecc-gui-elements';
import hierarchicalMappingChannel from '../store';

const MappingRule = React.createClass({

    mixins: [UseMessageBus],

    // define property types
    propTypes: {
    },

    // initilize state
    getInitialState() {
        return {
            ruleData: undefined,
        };
    },

    handleNavigate() {
        //hierarchicalMappingChannel
    },

    // template rendering
    render () {
        const loading = this.state.loading ? <Spinner /> : false;

        return (
            <div
                className="ecc-component-hierarchicalMapping__mappingRuleOverview__card"
            >
                Hello DI. I am Mapping Rule.
            </div>
        );
    },
});

export default MappingRule;