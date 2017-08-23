import React from 'react';
import ReactDOM from 'react-dom';

import HierarchicalMappingComponent from './HierarchicalMapping/HierarchicalMapping';

// eslint-disable-next-line
import SilkStore from './SilkStore/silkStore';

const hierarchicalMapping = (containerId, apiSettings) => {
    console.warn(containerId, apiSettings);
    ReactDOM.render(
        <HierarchicalMappingComponent {...apiSettings} />,
        document.getElementById(containerId)
    );
};

if (__DEBUG__) {
    hierarchicalMapping('react');
}

// TODO: @mhaschke, only import what we need?
require('ecc-gui-elements/src/main.scss');
require('./style/style.scss');

window.HierarchicalMapping = hierarchicalMapping;
