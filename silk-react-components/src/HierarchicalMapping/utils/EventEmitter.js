function eventEmitter(all) {
    all = all || Object.create(null);
    
    return {
        /**
         * Register an event handler for the given type.
         *
         * @param  {String} type	Type of event to listen for, or `"*"` for all events
         * @param  {Function} handler Function to call in response to given event
         * @memberOf mitt
         */
        on(type, handler) {
            (all[type] || (all[type] = [])).push(handler);
        },
        
        /**
         * Remove an event handler for the given type.
         *
         * @param  {String} type	Type of event to unregister `handler` from, or `"*"`
         * @param  {Function} handler Handler function to remove
         * @memberOf mitt
         */
        off(type, handler) {
            if (all[type]) {
                all[type].splice(all[type].indexOf(handler) >>> 0, 1);
            }
        },
        
        /**
         * Invoke all handlers for the given type.
         * If present, `"*"` handlers are invoked after type-matched handlers.
         *
         * @param {String} type  The event type to invoke
         * @param {Any} [evt]  Any value (object is recommended and powerful), passed to each handler
         * @memberOf mitt
         */
        emit(type, evt) {
            console.info('Emitted ' + type);
            (all[type] || []).slice().map((handler) => { handler(evt); });
            (all['*'] || []).slice().map((handler) => { handler(type, evt); });
        }
    };
}

const EventEmitter = eventEmitter();
export default EventEmitter;
