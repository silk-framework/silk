/**
 * Bridges `history.block` prompts to a custom (modal) confirmation surface.
 *
 * The `history` library, whenever a blocked navigation's prompt callback returns a *string*
 * message, calls the history's `getUserConfirmation(message, callback)` SYNCHRONOUSLY on the very
 * next line (see history's `confirmTransitionTo`). The default `getUserConfirmation` is
 * `window.confirm`. This module lets a single feature route that one confirmation through its own
 * async surface (e.g. an in-app modal) instead — WITHOUT hijacking confirmations from any other
 * `history.block` caller: the feature registers a one-shot handler immediately before returning its
 * prompt message; `getUserConfirmation` consumes that handler for exactly that navigation and
 * otherwise falls back to `window.confirm`.
 */

/** Resolves to the user's decision: `true` to proceed with the navigation, `false` to cancel. */
type ConfirmationHandler = () => Promise<boolean>;

// One-shot handler for the NEXT getUserConfirmation call. Set synchronously inside a block prompt
// callback and consumed synchronously by getUserConfirmation, so it is never left dangling across
// unrelated navigations.
let pendingHandler: ConfirmationHandler | null = null;

/**
 * Route the NEXT `getUserConfirmation` call through `handler` instead of `window.confirm`.
 *
 * Call this from a `history.block` prompt callback right before it returns its prompt message
 * string. The block callback and `getUserConfirmation` run back-to-back and synchronously, so the
 * handler is guaranteed to be consumed for exactly that navigation.
 */
export const routeNextBlockConfirmation = (handler: ConfirmationHandler): void => {
    pendingHandler = handler;
};

/**
 * `getUserConfirmation` to install on the app history (`createBrowserHistory({ getUserConfirmation })`).
 *
 * Uses the one-shot handler registered via {@link routeNextBlockConfirmation} when present (i.e. the
 * navigation that is being confirmed came from a block that opted in), otherwise the native
 * `window.confirm` — preserving default behavior for every other caller.
 */
export const getUserConfirmation = (message: string, callback: (confirmed: boolean) => void): void => {
    const handler = pendingHandler;
    pendingHandler = null;
    if (handler) {
        handler().then(callback, () => callback(false));
    } else {
        // eslint-disable-next-line no-alert
        callback(window.confirm(message));
    }
};
