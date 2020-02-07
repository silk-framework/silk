import { getTaskMetadataAsync } from "@ducks/shared/thunks/metadata.thunk";
import { addProjectPrefix, getProjectPrefixes, removeProjectPrefixes } from "@ducks/shared/thunks/widgets.thunk";

export default {
    getTaskMetadataAsync,
    getProjectPrefixes,
    addProjectPrefix,
    removeProjectPrefixes
}
