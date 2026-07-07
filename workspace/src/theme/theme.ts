import Store from "store";
import { ApplicationContainerProps } from "../../../libs/gui-elements";

/** The available theme modes. "auto" follows the operating system / browser preference. */
export type ThemeMode = ApplicationContainerProps["themeMode"];

const THEME_STORAGE_KEY = "themeMode";
const DEFAULT_THEME_MODE: ThemeMode = "light";

const isThemeMode = (value: unknown): value is ThemeMode => value === "light" || value === "dark" || value === "auto";

/** Fetches the configured theme mode from local storage, defaulting to "auto". */
export const fetchStoredThemeMode: () => ThemeMode = () => {
    const stored = Store.get(THEME_STORAGE_KEY, DEFAULT_THEME_MODE);
    return isThemeMode(stored) ? stored : DEFAULT_THEME_MODE;
};

/** Persists the chosen theme mode in local storage. */
export const setStoredThemeMode: (mode: ThemeMode) => void = (mode) => {
    Store.set(THEME_STORAGE_KEY, mode);
};
