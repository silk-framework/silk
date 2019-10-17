import React from 'react';
import { autocompleteAsync } from '../store';
import _ from 'lodash';

export class PropertyTypeInfo extends React.Component {
    state = {
        name: this.props.name,
        option: this.props.option,
        result: false,
    };
    
    componentDidMount() {
        autocompleteAsync({
            entity: 'propertyType',
            input: this.props.name,
            ruleId: null,
        }).subscribe(
            response => {
                this.setState({
                    result: _.get(
                        response,
                        ['options', '0', this.props.option],
                        this.props.name
                    ),
                });
            },
            () => {
                if (__DEBUG__) {
                    console.warn(`No ${
                        this.props.option
                        } found for the property type ${this.props.name}`);
                }
                this.setState({
                    result: this.props.name,
                });
            }
        );
    }
    
    render() {
        let text = this.state.result;
        if (this.props.appendedText) {
            text += this.props.appendedText;
        }
        return <div>{text}</div>;
    }
}
