import api from "./axios.js";

// BillingKey
export const registerBillingKey = (customerUid, cardName, cardNumber) =>
    api.post("/api/v1/subscriptions/billing-keys", { customerUid, cardName, cardNumber });

export const getBillingKeys = () =>
    api.get("/api/v1/subscriptions/billing-keys");

export const deleteBillingKey = (billingKeyId) =>
    api.delete(`/api/v1/subscriptions/billing-keys/${billingKeyId}`);

// Subscription
export const startSubscription = (billingKeyId, plan) =>
    api.post("/api/v1/subscriptions", { billingKeyId, plan });

export const getMySubscription = () =>
    api.get("/api/v1/subscriptions/me");

export const cancelSubscription = () =>
    api.post("/api/v1/subscriptions/me/cancel");

export const getMySubscriptionBillings = () =>
    api.get("/api/v1/subscriptions/me/billings");