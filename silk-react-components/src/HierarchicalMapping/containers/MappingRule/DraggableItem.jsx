import React from 'react';
import { Draggable } from 'react-beautiful-dnd';
import { MappingRule } from './MappingRule';
import { ScrollingHOC } from '@eccenca/gui-elements';

import { URI } from 'ecc-utils';
import { MAPPING_RULE_TYPE_OBJECT } from '../../utils/constants';

const isPasted = (id) => {
    const pastedId = sessionStorage.getItem('pastedId');
    return (pastedId !== null) && (pastedId === id);
};

const isExpanded = (id) => {
    const uriTemplate = new URI(window.location.href);
    if (uriTemplate.segment(-2) === 'rule') {
        return uriTemplate.segment(-1) === id
    }
};

class DraggableItem extends React.Component {
    
    state = {
        isPasted: isPasted(this.props.id),
        expanded: isExpanded(this.props.id) || isPasted(this.props.id)
    };
    
    componentDidMount() {
        if (this.state.isPasted) {
            !sessionStorage.removeItem('pastedId');
            this.props.scrollIntoView();
        }
    }
    
    handleExpand = (expanded = !this.state.expanded, id = true) => {
        // only trigger state / render change if necessary
        if (
            expanded !== this.state.expanded &&
            this.props.type !== MAPPING_RULE_TYPE_OBJECT &&
            (id === true || id === this.props.id)
        ) {
            this.setState({
                expanded
            });
        }
    };
    
    render() {
        return (
            <Draggable
                isDragDisabled={this.state.expanded}
                style={{ width: '15' }}
                key={this.props.id}
                draggableId={this.props.id}
            >
                {
                    (provided, snapshot) => <MappingRule
                        provided={provided}
                        snapshot={snapshot}
                        isPasted={this.state.isPasted}
                        expanded={this.state.expanded}
                        onExpand={this.handleExpand}
                        {...this.props}
                    />
                }
            </Draggable>
        )
    }
}

export default ScrollingHOC(DraggableItem);
