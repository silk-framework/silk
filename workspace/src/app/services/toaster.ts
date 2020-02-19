import Toaster from "@wrappers/bluprint/toaster";
import { Position } from "@wrappers/bluprint/constants";

/** Singleton toaster instance. Create separate instances for different options. */
export const AppToaster = Toaster.create({
    className: "toaster-app",
    position: Position.TOP,
});
