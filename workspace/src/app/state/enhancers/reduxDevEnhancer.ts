import { initialStore } from "../store.dto";

const reduxDevEnhancer = (createStore) => (
    reducer,
    initialState,
    enhancer
) => {
    const reduxDevReducer = (state, action) => {
        let newState;
        if (action.type === '__dev__/RESET_STORE') {
            newState = initialStore();
            newState.router = {
                ...state.router
            };

        } else {
            newState = reducer(state, action);
        }
        return newState;
    };

    return createStore(reduxDevReducer, initialState, enhancer)
};

export default reduxDevEnhancer;
