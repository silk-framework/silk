/**
 * This function can be helpful
 * if we want to add extra reducers for development environment
 * @param createStore
 */
const reduxDevEnhancer = (createStore) => (
    reducer,
    initialState,
    enhancer
) => {
    const reduxDevReducer = (state, action) => {
        return reducer(state, action);
    };

    return createStore(reduxDevReducer, initialState, enhancer)
};

export default reduxDevEnhancer;
