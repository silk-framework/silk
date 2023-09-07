import { useEffect } from "react";

interface IProps {
    // The hotkey to support, e.g. "ctrl+e"
    hotkey?: string;
    // The event handler for the hot key combination. Return false if the event should not bubble up.
    handler: (e: any) => any;
    // If the hot key should be active. If not set, the hot key will be active.
    enabled?: boolean;
    // Make the hot key trigger on a specific event type only, else it will be decided based on the hotkey.
    eventType?: EventType;
}
const Mousetrap = require("mousetrap");

type EventType = "keydown" | "keyup" | "keypress";
/**
 * Adds hotkey handling to a component.
 */
export default function useHotKey({ hotkey, handler, enabled = true, eventType }: IProps) {
    useEffect(() => {
        if (hotkey && enabled) {
            Mousetrap.bind(hotkey, handler, eventType);
            return () => {
                Mousetrap.unbind(hotkey);
            };
        }
    }, [hotkey, handler, enabled]);
}

/** Triggers the function that is registered for this hotkey. */
export function triggerHotkeyHandler(hotkey: string): void {
    if (hotkey) {
        Mousetrap.trigger(hotkey);
    }
}
