import React from "react";
import MessageHandler from "../../../src/app/views/pages/MappingEditor/HierarchicalMapping/components/MessageHandler";
import {errorChannel} from "../../../src/app/views/pages/MappingEditor/HierarchicalMapping/store";
import {render, waitFor} from "@testing-library/react";
import {findAllDOMElements, findElement} from "../../integration/TestHelper";
import {CLASSPREFIX, NotificationProps} from "@eccenca/gui-elements"

const getWrapper = () => render(<MessageHandler />);
export const notificationSelector = (intent: NotificationProps["intent"]) => `.${CLASSPREFIX}-notification.${CLASSPREFIX}-intent--${intent}`

describe("MessageHandler Component", () => {
    describe("on component mounted,", () => {
        it("should render Alert component, when `errorType` is equal to `alert`", async () => {
            const wrapper = getWrapper();
            errorChannel.subject("message.alert").onNext({ errorType: "alert", message: "lorem" });
            await waitFor(() => {
                expect(findAllDOMElements(wrapper, notificationSelector("neutral")).length).toBeGreaterThan(0);
                expect(findElement(wrapper, notificationSelector("neutral")).textContent).toContainHTML("lorem")
            });
        });

        it("should render Error component, when `errorType` is equal to `error`", async () => {
            const wrapper = getWrapper();
            errorChannel.subject("message.error").onNext({ errorType: "error", message: "lorem" });
            await waitFor(() => {
                findElement(wrapper, notificationSelector("danger"));
            });
        });

        it("should render Info component, when `errorType` is equal to `info`", async () => {
            const wrapper = getWrapper();
            errorChannel.subject("message.info").onNext({ errorType: "info", message: "lorem" });
            await waitFor(() => {
                findElement(wrapper, notificationSelector("info"));
            });
        });

        it("should render Success component, when `errorType` is equal to `success`", async () => {
            const wrapper = getWrapper();
            errorChannel.subject("message.success").onNext({ errorType: "success", message: "lorem" });
            await waitFor(() => {
                findElement(wrapper, notificationSelector("success"));
            });
        });

        it("should render Warning component, when `errorType` is equal to `warning`", async () => {
            const wrapper = getWrapper();
            errorChannel.subject("message.warning").onNext({ errorType: "warning", message: "lorem" });
            await waitFor(() => {
                findElement(wrapper, notificationSelector("warning"));
            });
        });
    });
});
