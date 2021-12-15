import { getTaskMetadataAsync, updateTaskMetadataAsync } from "@ducks/shared/thunks/metadata.thunk";
import { getAutocompleteResultsAsync } from "@ducks/shared/thunks/autocomplete.thunk";
import { getDatasetTypesAsync } from "@ducks/shared/thunks/datasetTypes.thunk";

export default {
    getTaskMetadataAsync,
    getAutocompleteResultsAsync,
    updateTaskMetadataAsync,
    getDatasetTypesAsync,
};
