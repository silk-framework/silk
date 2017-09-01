// TODO: use this mixin from GUI elements later

import ReactDOM from 'react-dom';
import scrollIntoView from 'scroll-into-view';
import _ from 'lodash';

const ViewportScrolling = {
    scrollIntoView(options = {}) {
        /*
            options {
                animationTime: 500, // (optional) integer, time in milliseconds
                topOffset: 0, // (optional) integer, pixels to offset top alignment
                callbackFinished: function(result) {}, // (optional) function, result parameter is currently 'cancelled' or 'completed'
            }
        */
        const component = ReactDOM.findDOMNode(this);
        if (__DEBUG__) {
            console.log('scrolling element with a height of ' + component.scrollHeight);
        }
        scrollIntoView(
            component,
            {
                time: _.get(options, 'animationTime', 500),
                align: {
                    topOffset: _.get(options, 'topOffset', 0),
                }
            },
            function(result) {
                if (__DEBUG__) {
                    console.log('element scrolling ' + result + ', now at ' + component.getBoundingClientRect().top);
                }
                if (_.isFunction(options.callbackFinished)) {
                    options.callbackFinished(result);
                }
            }
        );
    },
}

export default ViewportScrolling;
