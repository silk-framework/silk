import { configureStore } from "@reduxjs/toolkit";
import commonOps from "../operations";
import { commonSlice } from "../commonSlice";
import { initialCommonState } from "../initialState";
import * as requests from "../requests";

describe("commonOps", () => {
    afterEach(() => {
        jest.restoreAllMocks();
        window.history.replaceState({}, "", "/");
    });

    test("honors the URL language in a fresh private session", async () => {
        localStorage.clear();
        window.history.replaceState({}, "", "/?lng=fr");
        jest.spyOn(requests, "requestInitFrontend").mockResolvedValue({
            ...initialCommonState().initialSettings,
            initialLanguage: "de",
        });
        jest.spyOn(requests, "requestArtefactList").mockResolvedValue({});
        const store = configureStore({ reducer: commonSlice.reducer });

        await store.dispatch(commonOps.fetchCommonSettingsAsync());

        expect(store.getState().locale).toBe("fr");
        expect(localStorage.getItem("i18nextLng")).toBe("fr");
        expect(localStorage.getItem("i18nUserChoice")).toBeNull();
    });

    test("uses the supported base language from a regional URL locale", async () => {
        localStorage.clear();
        window.history.replaceState({}, "", "/?lng=fr-FR");
        jest.spyOn(requests, "requestInitFrontend").mockResolvedValue({
            ...initialCommonState().initialSettings,
            initialLanguage: "de",
        });
        jest.spyOn(requests, "requestArtefactList").mockResolvedValue({});
        const store = configureStore({ reducer: commonSlice.reducer });

        await store.dispatch(commonOps.fetchCommonSettingsAsync());

        expect(store.getState().locale).toBe("fr");
        expect(localStorage.getItem("i18nextLng")).toBe("fr");
        expect(localStorage.getItem("i18nUserChoice")).toBeNull();
    });

    test("uses the backend language when the URL language is unsupported", async () => {
        localStorage.clear();
        window.history.replaceState({}, "", "/?lng=zz");
        jest.spyOn(requests, "requestInitFrontend").mockResolvedValue({
            ...initialCommonState().initialSettings,
            initialLanguage: "de",
        });
        jest.spyOn(requests, "requestArtefactList").mockResolvedValue({});
        const store = configureStore({ reducer: commonSlice.reducer });

        await store.dispatch(commonOps.fetchCommonSettingsAsync());

        expect(store.getState().locale).toBe("de");
        expect(localStorage.getItem("i18nextLng")).toBe("de");
        expect(localStorage.getItem("i18nUserChoice")).toBeNull();
    });

    test("uses the backend language when i18next cached a language without a user choice", async () => {
        localStorage.clear();
        localStorage.setItem("i18nextLng", "en");
        jest.spyOn(requests, "requestInitFrontend").mockResolvedValue({
            ...initialCommonState().initialSettings,
            initialLanguage: "fr",
        });
        jest.spyOn(requests, "requestArtefactList").mockResolvedValue({});
        const store = configureStore({ reducer: commonSlice.reducer });

        await store.dispatch(commonOps.fetchCommonSettingsAsync());

        expect(store.getState().locale).toBe("fr");
        expect(localStorage.getItem("i18nextLng")).toBe("fr");
        expect(localStorage.getItem("i18nUserChoice")).toBeNull();
    });

    test("keeps a user-selected language when the backend has another default", async () => {
        localStorage.clear();
        localStorage.setItem("i18nUserChoice", "true");
        localStorage.setItem("i18nextLng", "de");
        window.history.replaceState({}, "", "/?lng=fr");
        jest.spyOn(requests, "requestInitFrontend").mockResolvedValue({
            ...initialCommonState().initialSettings,
            initialLanguage: "fr",
        });
        jest.spyOn(requests, "requestArtefactList").mockResolvedValue({});
        const store = configureStore({ reducer: commonSlice.reducer });

        await store.dispatch(commonOps.fetchCommonSettingsAsync());

        expect(store.getState().locale).toBe("de");
        expect(localStorage.getItem("i18nextLng")).toBe("de");
    });

    test("a backend language change does not mark the language as a user choice", async () => {
        localStorage.clear();
        let state = initialCommonState();
        const dispatch = (action: ReturnType<typeof commonSlice.actions.changeLanguage>) => {
            state = commonSlice.reducer(state, action);
        };

        await commonOps.changeLocale("fr")(dispatch);

        expect(state.locale).toBe("fr");
        expect(localStorage.getItem("i18nextLng")).toBe("fr");
        expect(localStorage.getItem("i18nUserChoice")).toBeNull();
    });

    test("a user language change marks the choice for DataManager", async () => {
        localStorage.clear();
        let state = initialCommonState();
        const dispatch = (action: ReturnType<typeof commonSlice.actions.changeLanguage>) => {
            state = commonSlice.reducer(state, action);
        };

        await commonOps.changeLocale("de", true)(dispatch);

        expect(state.locale).toBe("de");
        expect(localStorage.getItem("i18nextLng")).toBe("de");
        expect(localStorage.getItem("i18nUserChoice")).toBe("true");
    });

    test("buildStringValuedObject should convert all literal values to string values in a nested object", () => {
        const flatObject = {
            id: 1,
            root: true,
            source: {
                id: 2,
                name: "2",
                extra: {
                    id: 4,
                    name: "extra",
                },
            },
            target: {
                id: 3,
                name: "3",
            },
        };
        const expectedResult = {
            id: "1",
            root: "true",
            source: {
                id: "2",
                name: "2",
                extra: {
                    id: "4",
                    name: "extra",
                },
            },
            target: {
                id: "3",
                name: "3",
            },
        };
        expect(commonOps.buildStringValuedObject(flatObject)).toEqual(expectedResult);
    });

    test("extractParameterValues should remove the meta data fields and the dataset attributes", () => {
        const formValues = {
            label: "Some label",
            description: "Some description",
            id: "someId",
            tags: { selectedItems: [] },
            readOnly: true,
            uriProperty: "urn:uri",
            file: "some file",
            // Only root parameters are removed, nested parameters may have the same name
            objectParameter: { label: "nested label" },
        };
        expect(commonOps.extractParameterValues(formValues)).toStrictEqual({
            file: "some file",
            objectParameter: { label: "nested label" },
        });
    });

    test("extractParameterValues should keep a meta data field name that the plugin declares as a parameter", () => {
        const formValues = {
            label: "Some label",
            description: "A plugin parameter value, not the task description",
            id: "someId",
            tags: { selectedItems: [] },
            file: "some file",
        };
        // The plugin declares a parameter named 'description', so only the other meta data fields are stripped.
        const declaredParameters = { description: {}, file: {} };
        expect(commonOps.extractParameterValues(formValues, declaredParameters)).toStrictEqual({
            description: "A plugin parameter value, not the task description",
            file: "some file",
        });
    });
});
