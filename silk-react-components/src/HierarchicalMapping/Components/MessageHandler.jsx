// react
import React from 'react';
import _ from 'lodash';
import rxmq from 'ecc-messagebus';

import UseMessageBus from '../UseMessageBusMixin';

import { Nothing, Alert, Error, Info, Success, Warning } from '@eccenca/gui-elements';

const renderClasses = {
	alert: Alert,
	error: Error,
	info: Info,
	success: Success,
	warning: Warning
};

const errorChannel = rxmq.channel('errors');


const MessageHandler = React.createClass({
	mixins: [UseMessageBus],

	// initilize state
	getInitialState() {
		// return state
		return {
			errorMessages: []
		};
	},
	componentDidMount() {
		// listen for graphs loading
		this.subscribe(errorChannel.subject('message'), this.onError.bind(this, 'alert'));
		this.subscribe(errorChannel.subject('message.alert'), this.onError.bind(this, 'alert'));
		this.subscribe(errorChannel.subject('message.error'), this.onError.bind(this, 'error'));
		this.subscribe(errorChannel.subject('message.info'), this.onError.bind(this, 'info'));
		this.subscribe(errorChannel.subject('message.success'), this.onError.bind(this, 'success'));
		this.subscribe(errorChannel.subject('message.warning'), this.onError.bind(this, 'warning'));
	},

	// handle graphs loaded
	onError(errorType, data) {

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
				errorMessages
			});

		}

	},

	removeAfterDelay(type, key) {
		setTimeout(() => {
			this.removeMessage(key);
		}, 3000);
	},

	removeMessage(key) {
		const errorMessages = _.reject(this.state.errorMessages, ['key', key]);
		// apply to state
		this.setState({ errorMessages });
	},

	render() {
		const messages = this.state.errorMessages.map(({ message, errorType, key }, index) => {
			const Class = renderClasses[errorType];

			return (
				<Class
					key={'error_' + index}
					border={true}
					vertSpacing={true}
					handlerDismiss={this.removeMessage}
				>
					{message}
				</Class>
			);
		});

		return (messages.length > 0) ? (
			<div className='ecc-component-messagehandler'>
				{messages}
			</div>
		) : <Nothing />;
	}
});

export default MessageHandler;
