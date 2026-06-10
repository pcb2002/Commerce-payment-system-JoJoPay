import api from "./axios.js";

export const getPointBalance = () =>
    api.get("/api/v1/points/balance");

export const getPointHistories = () =>
    api.get("/api/v1/points/histories");