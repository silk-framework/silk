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
            <div>
                <RadioGroup
                    id="TargetCardinality"
                    className="ecc-silk-mapping__ruleseditor__isAttribute"
                    value={this.state.isAttribute ? 'single' : 'multiple'}
                    name = ""
                    onChange={({ value }) => { this.onChange(value === "single") }}
                >
                    <Radio
                        name="single"
                        value="single"
                        label={
                            <div>
                                <div id="ecc-target-cardinality-single">Write as a single value</div>
                                <div className="mdl-tooltip" data-mdl-for="ecc-target-cardinality-single">
                                    <div style={{textAlign: "left"}}>
                                        Write as a single value.
                                        <br/>
                                        This will have an effect on the following datasets:
                                        <br/>
                                        <b>XML:</b> Values will be written as an attribute.
                                        <br/>
                                        <b>JSON:</b> No arrays will be used for writing values. If multiple values are generated, it will fail.
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
                                <div id="ecc-target-cardinality-multiple">Write as multiple values</div>
                                <div className="mdl-tooltip" data-mdl-for="ecc-target-cardinality-multiple">
                                    <div style={{textAlign: "left"}}>
                                        Write as multiple values.
                                        <br/>
                                        This will have an effect on the following datasets:
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
