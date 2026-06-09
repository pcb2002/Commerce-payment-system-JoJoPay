// src/router/Router.jsx

import { Routes, Route } from 'react-router-dom';
import LoginPage from "../pages/auth/LoginPage.jsx";
import SignupPage from "../pages/auth/SignupPage.jsx";
import ProductListPage from "../pages/product/ProductListPage.jsx";
import ProductDetailPage from "../pages/product/ProductDetailPage.jsx";
import CartPage from "../pages/cart/CartPage.jsx";
import OrderNewPage from "../pages/order/OrderNewPage.jsx";
import OrderListPage from "../pages/order/OrderListPage.jsx";
import OrderDetailPage from "../pages/order/OrderDetailPage.jsx";
import PaymentPage from "../pages/payment/PaymentPage.jsx";
import RefundPage from "../pages/refund/RefundPage.jsx";
import SubscriptionPage from "../pages/subscription/SubscriptionPage.jsx";
import PointPage from "../pages/point/PointPage.jsx";
import MyProfilePage from "../pages/my/MyProfilePage.jsx";
import MyRefundListPage from "../pages/my/MyRefundListPage.jsx";

export default function Router() {
        return (
            <Routes>
                    {/* Auth */}
                    <Route path="/" element={<LoginPage />} />
                    <Route path="/login" element={<LoginPage />} />
                    <Route path="/signup" element={<SignupPage />} />

                    {/* Product */}
                    <Route path="/products" element={<ProductListPage />} />
                    <Route path="/products/:productId" element={<ProductDetailPage />} />

                    {/* Cart */}
                    <Route path="/cart" element={<CartPage />} />

                    {/* Order */}
                    <Route path="/orders" element={<OrderListPage />} />
                    <Route path="/orders/new" element={<OrderNewPage />} />
                    <Route path="/orders/:orderNumber" element={<OrderDetailPage />} />

                    {/* Payment */}
                    <Route path="/payment/result" element={<PaymentPage />} />

                    {/* Refund */}
                    <Route path="/orders/:orderId/refund" element={<RefundPage />} />

                    {/* Subscription */}
                    <Route path="/subscriptions" element={<SubscriptionPage />} />

                    {/* Point */}
                    <Route path="/points" element={<PointPage />} />

                    {/* My */}
                    <Route path="/my/profile" element={<MyProfilePage />} />
                    <Route path="/my/refunds" element={<MyRefundListPage />} />
            </Routes>
        );
}