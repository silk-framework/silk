import React from "react";
import { useTheme } from "next-themes";
import { useTranslation } from "react-i18next";
import { cn } from "@eccenca/gui-elements";

const THEMES = ["light", "dark"] as const;

/**
 * Light/dark appearance switcher for the sidebar user menu, rendered as the same compact
 * segmented control as the language switcher. Delegates to next-themes (`ThemeProvider
 * attribute="class"` in `index.tsx`), which toggles the `dark` class on `<html>` and persists
 * the choice under the `diapp-theme` storage key.
 */
export function ThemeSwitcher() {
    const [t] = useTranslation();
    const { theme, setTheme } = useTheme();

    return (
        <div
            role="group"
            aria-label={t("navigation.user.appearance", "Appearance")}
            className="flex w-full gap-0.5 rounded-md bg-muted p-0.5"
        >
            {THEMES.map((mode) => {
                const active = mode === (theme ?? "light");
                return (
                    <button
                        key={mode}
                        type="button"
                        onClick={() => setTheme(mode)}
                        aria-pressed={active}
                        className={cn(
                            "flex-1 rounded-sm px-2 py-1 text-xs font-medium transition-colors",
                            active
                                ? "bg-card text-foreground shadow-sm"
                                : "text-muted-foreground hover:text-foreground",
                        )}
                    >
                        {mode === "light"
                            ? t("navigation.user.themeLight", "Light")
                            : t("navigation.user.themeDark", "Dark")}
                    </button>
                );
            })}
        </div>
    );
}
