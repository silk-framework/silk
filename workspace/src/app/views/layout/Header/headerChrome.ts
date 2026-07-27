import { cn } from "@eccenca/gui-elements";

// Declares the header-chrome z-index tokens on :root (see headerChrome.css). Imported here so any
// consumer of these exports (Header, NotificationsMenu) also pulls in the token declarations.
import "./headerChrome.css";

/**
 * Ghost icon-button style shared by the header chrome actions (Help, Notifications) so they read as
 * one 36px control group aligned with the Create button. Icons inside are rendered `small` (16px) to
 * match the dense header chrome. Single source for the class so `NotificationsMenu` no longer keeps a
 * hand-synced copy.
 */
export const headerActionButtonClass = cn(
    "relative flex size-9 cursor-pointer items-center justify-center rounded-lg text-foreground transition-colors",
    "hover:bg-muted focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/40",
    "data-[state=open]:bg-muted",
);

/**
 * Menus opened from header (and sidebar) triggers must sit above the header chrome. Radix copies the
 * content's computed z-index onto its popper wrapper, and the header is `z-[var(--z-app-header)]`
 * (over modals via `--z-app-header-over-modals`), so the default shadcn `z-50` content would paint
 * behind the header where the menu overlaps the header band. This lifts header menus above that so
 * they connect to their trigger instead of reading as detached.
 */
export const headerMenuElevation = "z-[var(--z-app-header-menu)]";
