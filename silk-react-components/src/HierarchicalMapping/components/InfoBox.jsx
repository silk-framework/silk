import React from 'react';
import { Button } from '@eccenca/gui-elements';

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
                <Button
                    className="ecc-silk-mapping__rulesviewer__infobox-toggler"
                    iconName={
                        this.state.expanded ? 'expand_more' : 'chevron_right'
                    }
                    tooltip={this.state.expanded ? 'Show less' : 'Show more'}
                    onClick={this.toggleExpander}
                />
                <div className="ecc-silk-mapping__rulesviewer__infobox-content">
                    {this.props.children}
                </div>
            </div>
        );
    }
}
