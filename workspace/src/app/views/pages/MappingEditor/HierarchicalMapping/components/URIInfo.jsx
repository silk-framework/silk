import React from "react";
import _ from "lodash";
import { getApiDetails, getVocabInfoAsync } from "../store";
import { NotAvailable } from "gui-elements-deprecated";

export class URIInfo extends React.Component {
    state = {
        info: false,
    };

    componentDidMount() {
        if (getApiDetails().transformTask) {
            this.loadData(this.props);
        }
    }

    componentDidUpdate(prevProps) {
        if (!_.isEqual(this.props, prevProps)) {
            this.loadData(this.props);
        }
    }

    loadData(props) {
        const { uri, field } = props;
        getVocabInfoAsync(uri, field).subscribe(
            ({ info }) => {
                this.setState({ info });
            },
            () => {
                this.setState({ info: false });
            },
        );
    }

    render() {
        const { info } = this.state;

        if (info) {
            return <span>{info}</span>;
        }

        const { uri, fallback, field, ...otherProps } = this.props;

        let noInfo = false;

        if (fallback !== undefined) {
            noInfo = fallback;
        } else if (!_.isString(uri)) {
            noInfo = <NotAvailable />;
        } else if (field === "label") {
            const lastHash = uri.lastIndexOf("#");
            const lastSlash = lastHash === -1 ? uri.lastIndexOf("/") : lastHash;
            noInfo = uri.substring(lastSlash + 1).replace(/[<>]/g, "");
        }

        return (
            <span className="ecc-silk-mapping__uriinfo" {...otherProps}>
                {noInfo}
            </span>
        );
    }
}
