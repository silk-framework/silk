import _ from 'lodash';

const UseMessageBus = {
    // FIXME: Replace by general logging component once available
    useMessageBusLog(action, subject, ...args) {
        // Remove logging in production mode
        if (__DEBUG__) {
            const channel =
                this.messageBusID ||
                this.context.privateMessageBusID ||
                subject.defaultChannel ||
                'unknown';
            const subjName = subject.subject || subject.name;

            switch (action) {
                case 'onNext':
                    console.log(
                        `UseMessageBus: ${this.constructor
                            .displayName} sent on ${channel}:${subjName}`,
                        ...args,
                    );
                    break;
                case 'subscribe':
                    console.log(
                        `UseMessageBus: ${this.constructor
                            .displayName} subscribed on ${channel}:${subjName}`,
                    );
                    break;
                default:
                    throw new Error(`UseMessageBus ${action} not defined`);
            }
        }
    },
    // retrieve subject if created by ecc-messagebus.createChannels
    getSubject(subject) {
        if (_.isFunction(subject.getSubject)) {
            const channel =
                this.messageBusID || this.context.privateMessageBusID;
            return subject.getSubject(channel);
        }
        return subject;
    },
    // send message on subject
    onNext(subject, ...args) {
        const currentSubject = this.getSubject(subject);

        this.useMessageBusLog('onNext', subject, ...args);

        currentSubject.onNext(...args);
    },
    // subscribe on subject
    subscribe(subject, ...args) {
        const currentSubject = this.getSubject(subject);

        this.useMessageBusLog('subscribe', subject);

        const subscription = currentSubject.subscribe(...args);
        if (!this.subscriptionsID) {
            this.subscriptionsID = _.uniqueId('subscriptions-');
            this[this.subscriptionsID] = [];
        }
        // add to subscriptions list
        this[this.subscriptionsID].push(subscription);
    },
    componentWillUnmount() {
        // unsubscribe all subscriptions on unmount
        if (this.subscriptionsID) {
            this[this.subscriptionsID].map(sub => sub.dispose());
            this[this.subscriptionsID] = [];
        }
    },
};

export default UseMessageBus;
