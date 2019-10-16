// react
import React from 'react';
import _ from 'lodash';
import rxmq from 'ecc-messagebus';

import { Alert, Error, Info, Success, Warning } from '@eccenca/gui-elements';

const RENDER_CLASSES = {
    alert: Alert,
    error: Error,
    info: Info,
    success: Success,
    warning: Warning,
};

const errorChannel = rxmq.channel('errors');

class MessageHandler extends React.Component {
    state = {
        errorMessages: [],
    };

    componentDidMount() {
		// listen for graphs loading
        errorChannel.subject('message').subscribe(data => this.onError('alert', data));
        errorChannel.subject('message.alert').subscribe(data => this.onError('alert', data));
        errorChannel.subject('message.error').subscribe(data => this.onError('error', data));
        errorChannel.subject('message.info').subscribe(data => this.onError('info', data));
        errorChannel.subject('message.success').subscribe(data => this.onError('success', data));
        errorChannel.subject('message.warning').subscribe(data => this.onError('warning', data));
    }

	// handle graphs loaded
    onError = (errorType, data) => {
		// get current messages
        const { errorMessages } = this.state;
        const messageKey = _.uniqueId('messageHandler--message-');
        const result = { message: _.isString(data) ? data : data.message };

        if (data.response && data.response.type === 'application/json') {
            try {
                const body = JSON.parse(data.response.text);
                result.message = body.message || result.message;
            } catch (syntax) {
            }
        }

		// assign errorType
        result.errorType = errorType;
        result.key = messageKey;

		// prevent doublettes (same type/same message)
        if (!_.some(errorMessages, { message: result.message, errorType })) {
            errorMessages.unshift(result);

            this.removeAfterDelay(errorType, messageKey);

			// apply to state
            this.setState({
                errorMessages,
            });
        }
    };

    removeAfterDelay(type, key) {
        setTimeout(() => {
            this.removeMessage(key);
        }, 3000);
    }

    removeMessage = key => {
        const errorMessages = _.reject(this.state.errorMessages, ['key', key]);
		// apply to state
        this.setState({ errorMessages });
    };

    render() {
        const messages = this.state.errorMessages.map(({ message, errorType, key }, index) => {
            const Class = RENDER_CLASSES[errorType];

            return (
                <Class
                    key={`error_${index}`}
                    border={true}
                    vertSpacing={true}
                    handlerDismiss={this.removeMessage}
                >
                    {message}
                </Class>
            );
        });

        return (messages.length > 0) ? (
            <div className="ecc-component-messagehandler">
                {messages}
            </div>
        ) : false;
    }
}

export default MessageHandler;
