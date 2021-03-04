import React from 'react';
import {
    Radio,
    RadioGroup,
} from '@eccenca/gui-elements';
import * as PropTypes from "prop-types";

class TargetCardinality extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            isAttribute: props.isAttribute,
        };
    }

    onChange(isAttribute) {
        this.setState({isAttribute: isAttribute});
        this.props.onChange(isAttribute);
    }

    render() {
        return (
            <div className="ecc-silk-mapping__ruleseditor__isAttribute">
                <RadioGroup
                    value={this.state.isAttribute ? 'single' : 'multiple'}
                    name = ""
                    onChange={({ value }) => { this.onChange(value === "single") }}
                >
                    <Radio
                        name="single"
                        value="single"
                        label={
                            <div>
                                <div id="ecc-target-cardinality-single">Single value</div>
                                <div className="mdl-tooltip" data-mdl-for="ecc-target-cardinality-single">
                                    <div style={{textAlign: "left"}}>
                                        A single value is expected for this property.
                                        Receiving multiple values will trigger a validation error.
                                        <br/>
                                        In addition, the following datasets will adapt the generated schema:
                                        <br/>
                                        <b>XML:</b> Values will be written as an attribute.
                                        <br/>
                                        <b>JSON:</b> No arrays will be used for writing values.
                                    </div>
                                </div>
                            </div>
                        }
                    />
                    <Radio
                        name="multiple"
                        value="multiple"
                        label={
                            <div>
                                <div id="ecc-target-cardinality-multiple">Multiple values</div>
                                <div className="mdl-tooltip" data-mdl-for="ecc-target-cardinality-multiple">
                                    <div style={{textAlign: "left"}}>
                                        Multiple values may be generated for this property.
                                        <br/>
                                        In addition, the following datasets will adapt the generated schema:
                                        <br/>
                                        <b>XML:</b> Values will be written as nested elements.
                                        <br/>
                                        <b>JSON:</b> Arrays will be used for writing values.
                                    </div>
                                </div>
                            </div>
                        }
                    />
                </RadioGroup>
            </div>
        )
    }
}

TargetCardinality.propTypes = {
    isAttribute: PropTypes.bool.isRequired,
    onChange: PropTypes.func
};

export default TargetCardinality;
