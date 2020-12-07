import { useEffect } from "react";

interface IProps {
    // The hotkey to support, e.g. "ctrl+e"
    hotkey: string;
    // The event handler for the hot key combination. Return false if the event should not bubble up.
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

/** Triggers the function that is registered for this hotkey. */
export function triggerHotkeyHandler(hotkey: string): void {
    if (hotkey) {
        Mousetrap.trigger(hotkey);
    }
}
