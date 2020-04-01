import React from 'react';
import _ from 'lodash';
import {
    Button,
    DisruptiveButton,
    Spinner,
} from '@eccenca/gui-elements';
import { URI } from 'ecc-utils';
import PropTypes from 'prop-types';

import { ruleRemoveAsync, setApiDetails } from './store';

import MappingsTree from './containers/MappingsTree';
import MappingsWorkview from './containers/MappingsWorkview';
import MessageHandler from './components/MessageHandler';
import { MAPPING_RULE_TYPE_OBJECT } from './utils/constants';
import { MESSAGES } from './utils/constants';
import RemoveMappingRuleDialog from './elements/RemoveMappingRuleDialog';
import DiscardChangesDialog from './elements/DiscardChangesDialog';
import EventEmitter from './utils/EventEmitter';
import { isDebugMode } from './utils/isDebugMode';

class EvaluateMapping extends React.Component {
    // define property types
    static propTypes = {
        baseUrl: PropTypes.string.isRequired, // DI API Base
        project: PropTypes.string.isRequired, // Current DI Project
        transformTask: PropTypes.string.isRequired, // Current Transformation
        initialRule: PropTypes.string.isRequired,
        offset: PropTypes.number.isRequired,
        limit: PropTypes.number.isRequired
    };

    constructor(props) {
        super(props);
        const {
            baseUrl, project, transformTask, initialRule,
        } = this.props;
        setApiDetails({
            baseUrl,
            project,
            transformTask,
        });

        this.state = {
            // currently selected rule id
            currentRuleId: _.isEmpty(initialRule) ? undefined : initialRule,
        };
    }

    // react to rule id changes
    onRuleNavigation = ({ newRuleId }) => {
        this.setState({
            currentRuleId: newRuleId,
        });
    };

    render() {
        return <div className="mdl-grid mdl-grid--no-spacing" style={{ "height": "100%" }}>
            <div className="mdl-cell mdl-cell--3-col">
                <MappingsTree
                    currentRuleId={this.state.currentRuleId}
                    handleRuleNavigation={this.onRuleNavigation}
                />
            </div>
            <iframe id = "generatedEntitiesIFrame"
                    className = "mdl-cell mdl-cell--9-col mdl-cell--stretch"
                    src = { `evaluate/generatedEntities?rule=${this.state.currentRuleId}&offset=${this.props.offset}&limit=${this.props.limit}` }
                    frameBorder = "0"
                    style={{"margin": "7px"}}
            />
        </div>
    }
}

export default EvaluateMapping;
