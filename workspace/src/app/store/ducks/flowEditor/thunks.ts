import { createAsyncThunk } from "@reduxjs/toolkit";
import { requestConfigPorts } from "./requests";
import { EDITOR_ASYNC_TYPES } from "./typings";

/************* Editor *************/
//get port configuration
export const getConfigPorts = createAsyncThunk(EDITOR_ASYNC_TYPES.portConfiguration, requestConfigPorts);
