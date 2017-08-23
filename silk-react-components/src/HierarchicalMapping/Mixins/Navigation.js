// import _ from 'lodash';

import hierarchicalMappingChannel from '../store';

const Navigation = {
    // jumps to selected rule as new center of view
    handleNavigate(id, event) {
        hierarchicalMappingChannel
            .subject('ruleId.change')
            .onNext({newRuleId: id, parent: this.props.rule.id});

        event.stopPropagation();
    },
}

export default Navigation;
