import React from "react";
import { Elements } from "react-flow-renderer";

type utilsFuncType = (changeElements: React.Dispatch<React.SetStateAction<Elements<any>>>, something: () => void) => {};

const useAddStickyNote: utilsFuncType = (changeElements) => {
    const [currentStickyNote, setCurrentStickyNote] = React.useState<Map<string, string>>(new Map());

    React.useEffect(() => {
        setCurrentStickyNote(new Map());
        //Todo
    }, []);

    return {
        changeElements,
        currentStickyNote,
    };
};

export default useAddStickyNote;
