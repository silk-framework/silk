import React from "react";
import { getHistory } from "../store/configureStore";

export const useEditorPromptHack = (hasUnsavedChanges: boolean) => {
    /** updated id unsaved status */
    React.useEffect(() => {
        const closeButton = document.querySelector("[data-test-id='close-project-tab-view']");
        closeButton?.addEventListener("click", (e: any) => {
            if (hasUnsavedChanges) {
                e.preventDefault();
                e.stopPropagation();
                const history = getHistory();
                history.replace({ search: `${window.location.search} ` }); //just to trigger the browser prompt
            }
        });
    }, [hasUnsavedChanges]);
};
