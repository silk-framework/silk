import { useEffect } from "react";

interface IProps {
    // The hotkey to support, e.g. "ctrl+e"
    hotkey: string;
    // The event handler for the hot key combination
    handler: () => void;
}
const Mousetrap = require("mousetrap");

/**
 * Adds hotkey handling to a component.
 */
export default function useHotKey({ hotkey, handler }: IProps) {
    useEffect(() => {
        if (hotkey && typeof hotkey === "string") {
            Mousetrap.bind(hotkey, handler);
            return () => {
                Mousetrap.unbind(hotkey);
            };
        }
    }, [hotkey]);
}
