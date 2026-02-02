import React from "react";
import ReactDOM from "react-dom";
import _ from "lodash";

// minimal replication of the original `ScrollingHOC` component from the deprecated GUI elements
export function ScrollingHOC(WrappedComponent) {
    return class extends React.Component {
        constructor(props) {
            super(props);
            this.scrollIntoView = this.scrollIntoView.bind(this);
        }

        scrollIntoView(options = {}) {
            this.scrollElementIntoView(ReactDOM.findDOMNode(this), options);

            /*
            options {
                topOffset: 0, // (optional) integer, pixels to offset top alignment
            }
            */
        }

        scrollElementIntoView(element, options = {}) {
            let domElement = false;
            if (_.isElement(element)) {
                // is already a DOM element
                domElement = element;
            } else if (_.get(element, "props", false) !== false) {
                // await a mounted react element or component
                domElement = ReactDOM.findDOMNode(element);
            }

            if (!domElement) {
                return false;
            }

            return domElement.scrollIntoView({
                behavior: "smooth",
                block: "nearest",
                container: "all",
            });
        }

        render() {
            return <WrappedComponent scrollIntoView={this.scrollIntoView} {...this.props} />;
        }
    };
}

export default ScrollingHOC;
