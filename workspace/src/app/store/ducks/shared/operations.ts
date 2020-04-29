import { getTaskMetadataAsync } from "@ducks/shared/thunks/metadata.thunk";
import { getAutocompleteResultsAsync } from "@ducks/shared/thunks/autocomplete.thunk";
import { getRelatedItemsAsync } from "@ducks/shared/thunks/relatedItems.thunk";

export default {
    getTaskMetadataAsync,
    getAutocompleteResultsAsync,
    getRelatedItemsAsync,
};
