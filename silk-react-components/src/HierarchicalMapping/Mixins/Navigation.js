// import _ from 'lodash';

import hierarchicalMappingChannel from '../store';
import { MESSAGES } from '../constants';

const Navigation = {
    // jumps to selected rule as new center of view
    handleNavigate(id, parent, event) {
        hierarchicalMappingChannel
            .subject(MESSAGES.RULE_ID.CHANGE)
            .onNext({newRuleId: id, parentId: parent});

        event.stopPropagation();
    },

    handleCreate(infoCreation) {
        hierarchicalMappingChannel
            .subject(MESSAGES.MAPPING.CREATE)
            .onNext(infoCreation);
    },

    handleShowSuggestions(event) {
        event.persist();
        hierarchicalMappingChannel
            .subject(MESSAGES.MAPPING.SHOW_SUGGESTIONS)
            .onNext(event);
    },

    handleToggleRuleDetails(stateExpand) {
        hierarchicalMappingChannel
            .subject(MESSAGES.TOGGLE_DETAILS)
            .onNext(stateExpand);
    },

    promoteToggleTreenavigation(stateVisibility) {
        hierarchicalMappingChannel
            .subject(MESSAGES.TREE_NAV.TOGGLE_VISIBILITY)
            .onNext(stateVisibility);
    },
};

export default Navigation;
