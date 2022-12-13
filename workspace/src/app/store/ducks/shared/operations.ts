import { getAutocompleteResultsAsync } from "@ducks/shared/thunks/autocomplete.thunk";
import { getDatasetTypesAsync } from "@ducks/shared/thunks/datasetTypes.thunk";
import { getTaskMetadataAsync, updateTaskMetadataAsync } from "@ducks/shared/thunks/metadata.thunk";

const sharedOps = {
    getTaskMetadataAsync,
    getAutocompleteResultsAsync,
    updateTaskMetadataAsync,
    getDatasetTypesAsync,
};

export default sharedOps;
