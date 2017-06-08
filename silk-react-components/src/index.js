import React from 'react';
import ReactDOM from 'react-dom';

import HierarchicalMappingComponent from './HierarchicalMapping/HierarchicalMapping.jsx';

const hierarchicalMapping = (containerId, apiUrl) => {
    ReactDOM.render(
        <HierarchicalMappingComponent api={apiUrl}/>,
        document.getElementById(containerId)
    );
};

if (__DEBUG__) {
    require('ecc-gui-elements/src/main.scss');
    hierarchicalMapping('react');
}

require('./style/style.scss');

window.HierarchicalMapping = hierarchicalMapping;
