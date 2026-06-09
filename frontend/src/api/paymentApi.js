import api from "./axios.js";

export const confirmPayment = (orderNumber, portonePaymentId) =>
    api.post("/api/v1/payments/confirm", { orderNumber, portonePaymentId });