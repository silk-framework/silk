// import _ from 'lodash';

import hierarchicalMappingChannel from '../store';

const Navigation = {
    // jumps to selected rule as new center of view
    handleNavigate(id, parent, event) {
        hierarchicalMappingChannel
            .subject('ruleId.change')
            .onNext({newRuleId: id, parentRuleId: parent});

        event.stopPropagation();
    },

    handleCreate(infoCreation) {
        hierarchicalMappingChannel
            .subject('mapping.create')
            .onNext(infoCreation);
    },

    handleShowSuggestions(event) {
        event.persist();
        hierarchicalMappingChannel
            .subject('mapping.showSuggestions')
            .onNext(event);
    },

    handleToggleRuleDetails(stateExpand) {
        hierarchicalMappingChannel
            .subject('list.toggleDetails')
            .onNext(stateExpand);
    },

    promoteToggleTreenavigation(stateVisibility) {
        hierarchicalMappingChannel
            .subject('treenav.toggleVisibility')
            .onNext(stateVisibility);
    },
};

export default Navigation;
