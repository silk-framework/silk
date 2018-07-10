import React from 'react';
import ReactDOM from 'react-dom';

import HierarchicalMappingComponent from './HierarchicalMapping/HierarchicalMapping';

// eslint-disable-next-line
import SilkStore from './SilkStore/silkStore';

const hierarchicalMapping = (containerId, apiSettings) => {
    ReactDOM.render(
        <HierarchicalMappingComponent {...apiSettings} />,
        document.getElementById(containerId)
    );
};

require('./style/style.scss');

window.HierarchicalMapping = hierarchicalMapping;