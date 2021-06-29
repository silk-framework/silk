import { requestSearchList } from "@ducks/workspace/requests";
import { createAsyncThunk } from "@reduxjs/toolkit";
import { requestConfigPorts } from "./requests";
import { EDITOR_ASYNC_TYPES } from "./typings";

/************* SideBar *************/

// get all task for sidebar
export const requestSearchTask = createAsyncThunk(EDITOR_ASYNC_TYPES.searchList, requestSearchList);

/************* Editor *************/

//get port configuration
export const getConfigPorts = createAsyncThunk(EDITOR_ASYNC_TYPES.portConfiguration, requestConfigPorts);
