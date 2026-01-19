import React from 'react';
import { IconButton } from "@eccenca/gui-elements"

export class InfoBox extends React.Component {
    state = {
        expanded: false,
    };

    toggleExpander = event => {
        event.stopPropagation();
        this.setState({
            expanded: !this.state.expanded,
        });
    };

    render() {
        return (
            <div
                className={`ecc-silk-mapping__rulesviewer__infobox${
                    !this.state.expanded ? ' is-narrowed' : ''
                    }`}
            >
                <IconButton
                    className="ecc-silk-mapping__rulesviewer__infobox-toggler"
                    name={
                        this.state.expanded ? 'toggler-showless' : 'toggler-showmore'
                    }
                    text={this.state.expanded ? 'Show less' : 'Show more'}
                    onClick={this.toggleExpander}
                    size={"small"}
                />
                <div className="ecc-silk-mapping__rulesviewer__infobox-content">
                    {this.props.children}
                </div>
            </div>
        );
    }
}
