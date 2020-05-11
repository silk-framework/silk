import { requestAutocompleteResults } from "@ducks/shared/requests";

export const getAutocompleteResultsAsync = async (payload) => {
    try {
        return await requestAutocompleteResults({
            ...payload,
            limit: 10000,
            offset: 0,
        });
    } catch (e) {
        return e;
    }
};
