import React from 'react';
import ReactDOM from 'react-dom';

import HierarchicalMappingComponent from './HierarchicalMapping/HierarchicalMapping.jsx';
import SilkStore from './SilkStore/silkStore'

const hierarchicalMapping = (containerId, apiSettings) => {
    console.warn(containerId, apiSettings);
    ReactDOM.render(
        <HierarchicalMappingComponent
            {...apiSettings}
        />,
        document.getElementById(containerId)
    );
};

if (__DEBUG__) {
    require('ecc-gui-elements/src/main.scss');
    hierarchicalMapping('react');
}

require('./style/style.scss');

window.HierarchicalMapping = hierarchicalMapping;
