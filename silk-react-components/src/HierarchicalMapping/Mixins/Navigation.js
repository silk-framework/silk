// import _ from 'lodash';

import hierarchicalMappingChannel from '../store';

const Navigation = {
    // jumps to selected rule as new center of view
    handleNavigate(id, parent, event) {
        hierarchicalMappingChannel
            .subject('ruleId.change')
            .next({newRuleId: id, parentId: parent});

        event.stopPropagation();
    },

    handleCreate(infoCreation) {
        hierarchicalMappingChannel
            .subject('mapping.create')
            .next(infoCreation);
    },

    handleShowSuggestions(event) {
        event.persist();
        hierarchicalMappingChannel
            .subject('mapping.showSuggestions')
            .next(event);
    },

    handleToggleRuleDetails(stateExpand) {
        hierarchicalMappingChannel
            .subject('list.toggleDetails')
            .next(stateExpand);
    },

    promoteToggleTreenavigation(stateVisibility) {
        hierarchicalMappingChannel
            .subject('treenav.toggleVisibility')
            .next(stateVisibility);
    },
};

export default Navigation;
