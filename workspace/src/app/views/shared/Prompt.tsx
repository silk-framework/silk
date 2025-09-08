//Alternative to react router Prompt that existed in v5, now custom implemented
import React from "react";
import { useBlocker } from "react-router-dom";

interface PromptProps {
    //when the next location should be blocked
    when: boolean;
    // the message that the user sees
    message: string;
}

export const usePromptBlocker = (when: boolean, message: string) => {
    const blocker = useBlocker(when);

    return blocker.state === "blocked" ? (window.confirm(message) ? blocker.proceed() : blocker.reset()) : null;
};

export const Prompt: React.FC<PromptProps> = ({ when, message }) => {
    usePromptBlocker(when, message);
    return <></>;
};
