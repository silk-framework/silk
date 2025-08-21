import React from "react";
import MessageHandler from "../../../src/app/views/pages/MappingEditor/HierarchicalMapping/components/MessageHandler";
import { errorChannel } from "../../../src/app/views/pages/MappingEditor/HierarchicalMapping/store";
import { render, waitFor } from "@testing-library/react";
import { findElement } from "../../integration/TestHelper";

const getWrapper = (renderer = render) => renderer(<MessageHandler />);

describe("MessageHandler Component", () => {
    describe("on component mounted, ", () => {
        xit("should render Alert component, when `errorType` is equal to `alert`", async () => {
            const wrapper = getWrapper(render);
            errorChannel.subject("message.alert").publish({ errorType: "alert", message: "lorem" });

            findElement(wrapper, "[class*='mdl-alert']");
        });

        xit("should render Error component, when `errorType` is equal to `error`", () => {
            const wrapper = getWrapper(render);
            errorChannel.subject("message.error").publish({ errorType: "error", message: "lorem" });
            findElement(wrapper, "[class*='mdl-alert--danger']");
        });

        xit("should render Info component, when `errorType` is equal to `info`", () => {
            const wrapper = getWrapper(render);
            errorChannel.subject("message.info").publish({ errorType: "info", message: "lorem" });
            findElement(wrapper, "[class*='mdl-alert--info']");
        });

        xit("should render Success component, when `errorType` is equal to `success`", () => {
            const wrapper = getWrapper(render);
            errorChannel.subject("message.success").publish({ errorType: "success", message: "lorem" });
            findElement(wrapper, "[class*='mdl-alert--success']");
        });

        xit("should render Warning component, when `errorType` is equal to `warning`", () => {
            const wrapper = getWrapper(render);
            errorChannel.subject("message.warning").publish({ errorType: "warning", message: "lorem" });
            findElement(wrapper, "[class*='mdl-alert--warning']");
        });
    });
});
