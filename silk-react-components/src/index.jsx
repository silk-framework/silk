import React from 'react';
import ReactDom from 'react-dom';

import HierarchicalMappingComponent from './HierarchicalMapping/HierarchicalMapping';

// eslint-disable-next-line
import SilkStore from './SilkStore/silkStore';



require('./style/style.scss');

window.silkReactComponents = {
    hierarchicalMapping: (containerId, apiSettings) => {
        ReactDom.render(
            <HierarchicalMappingComponent {...apiSettings} />,
            document.getElementById(containerId)
        );
    }
};
