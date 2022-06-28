import React from 'react';
import _ from 'lodash';

import PropTypes from 'prop-types';
import {setApiDetails} from './store';
import MappingsTree from './containers/MappingsTree';

/**
 * Legacy mappings evaluation view.
 * Uses an inline iFrame to display the old evaluation tree.
 * Should be replaced in the future.
 */
class EvaluateMapping extends React.Component {
    // define property types
    static propTypes = {
        baseUrl: PropTypes.string.isRequired, // DI API Base
        project: PropTypes.string.isRequired, // Current DI Project
        transformTask: PropTypes.string.isRequired, // Current Transformation
        initialRule: PropTypes.string.isRequired, // Initially selected mapping rule
        offset: PropTypes.number.isRequired, // Entity offset
        limit: PropTypes.number.isRequired // Entity limit
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
