import React, { Component } from 'react';
import { logError } from "./services/errorLogger";
import { isDevelopment } from "./constants";

/**
 * Catch the children components errors
 * @see https://reactjs.org/blog/2017/07/26/error-handling-in-react-16.html
 * @see https://github.com/facebook/react/issues/11334#issuecomment-338656383
 */
class ErrorBoundary extends Component {

    getDerivedStateFromError() {

    }

    componentDidCatch(error, info) {
        logError(error, info);
        if (isDevelopment) {
            console.log(error, info);
        }
    }

    render() {
        return this.props.children;
    }
}

export default ErrorBoundary;
