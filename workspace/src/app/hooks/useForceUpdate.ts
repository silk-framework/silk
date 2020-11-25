import { useState } from "react";

export function useForceUpdate() {
    const setValue = useState(0)[1]; // integer state
    return () => setValue((value) => ++value); // update the state to force render
}
