import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { getCart, updateCartItemQuantity, deleteCartItem, clearCart } from '../../api/cartApi';

export default function CartPage() {
    const navigate = useNavigate();

    const [cart, setCart] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [updatingId, setUpdatingId] = useState(null);

    const fetchCart = async () => {
        setLoading(true);
        setError('');
        try {
            const data = await getCart();
            setCart(data);
        } catch (err) {
            setError('장바구니를 불러오는 데 실패했습니다.');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchCart();
    }, []);

    const handleQuantityChange = async (cartItemId, newQuantity) => {
        if (newQuantity < 1) return;
        setUpdatingId(cartItemId);
        try {
            await updateCartItemQuantity(cartItemId, newQuantity);
            await fetchCart();
        } catch (err) {
            alert('수량 변경에 실패했습니다.');
        } finally {
            setUpdatingId(null);
        }
    };

    const handleDelete = async (cartItemId) => {
        if (!window.confirm('해당 상품을 장바구니에서 삭제하시겠습니까?')) return;
        try {
            await deleteCartItem(cartItemId);
            await fetchCart();
        } catch (err) {
            alert('삭제에 실패했습니다.');
        }
    };

    const handleClear = async () => {
        if (!window.confirm('장바구니를 전체 비우시겠습니까?')) return;
        try {
            await clearCart();
            await fetchCart();
        } catch (err) {
            alert('장바구니 비우기에 실패했습니다.');
        }
    };

    if (loading) return (
        <div className="d-flex justify-content-center align-items-center" style={{ minHeight: '50vh' }}>
            <div className="spinner-border text-primary" role="status" />
        </div>
    );

    if (error) return (
        <div className="container py-4">
            <div className="alert alert-danger">{error}</div>
        </div>
    );

    const isEmpty = !cart || cart.cartItems.length === 0;

    return (
        <div className="container py-4" style={{ maxWidth: '720px' }}>
            <div className="d-flex justify-content-between align-items-center mb-3">
                <h2 className="h5 fw-semibold mb-0">장바구니</h2>
                {!isEmpty && (
                    <button className="btn btn-outline-danger btn-sm" onClick={handleClear}>
                        전체 비우기
                    </button>
                )}
            </div>

            {isEmpty ? (
                <div className="card">
                    <div className="card-body text-center py-5 text-muted">
                        <p className="mb-3">장바구니가 비어 있습니다.</p>
                        <button className="btn btn-primary btn-sm" onClick={() => navigate('/products')}>
                            상품 보러가기
                        </button>
                    </div>
                </div>
            ) : (
                <>
                    <div className="card mb-3">
                        <ul className="list-group list-group-flush">
                            {cart.cartItems.map((item) => (
                                <li key={item.cartItemId} className="list-group-item py-3">
                                    <div className="d-flex justify-content-between align-items-start gap-3">
                                        {/* 상품 정보 */}
                                        <div className="flex-grow-1" style={{ cursor: 'pointer' }} onClick={() => navigate(`/products/${item.productId}`)}>
                                            <p className="fw-medium mb-1">{item.productName}</p>
                                            <p className="text-muted small mb-0">{item.productPrice.toLocaleString()}원 / 개</p>
                                        </div>

                                        {/* 수량 조절 */}
                                        <div className="d-flex align-items-center gap-2">
                                            <button
                                                className="btn btn-outline-secondary btn-sm"
                                                style={{ width: '30px', padding: '0' }}
                                                disabled={updatingId === item.cartItemId || item.quantity <= 1}
                                                onClick={() => handleQuantityChange(item.cartItemId, item.quantity - 1)}
                                            >
                                                −
                                            </button>
                                            <span style={{ minWidth: '24px', textAlign: 'center' }}>
                        {updatingId === item.cartItemId
                            ? <span className="spinner-border spinner-border-sm" />
                            : item.quantity}
                      </span>
                                            <button
                                                className="btn btn-outline-secondary btn-sm"
                                                style={{ width: '30px', padding: '0' }}
                                                disabled={updatingId === item.cartItemId}
                                                onClick={() => handleQuantityChange(item.cartItemId, item.quantity + 1)}
                                            >
                                                +
                                            </button>
                                        </div>

                                        {/* 합계 금액 */}
                                        <div className="text-end" style={{ minWidth: '90px' }}>
                                            <p className="fw-semibold mb-1">{item.totalPrice.toLocaleString()}원</p>
                                            <button
                                                className="btn btn-link btn-sm text-danger p-0 text-decoration-none"
                                                onClick={() => handleDelete(item.cartItemId)}
                                            >
                                                삭제
                                            </button>
                                        </div>
                                    </div>
                                </li>
                            ))}
                        </ul>
                    </div>

                    {/* 결제 요약 */}
                    <div className="card">
                        <div className="card-body">
                            <div className="d-flex justify-content-between align-items-center mb-3">
                                <span className="text-muted">총 상품 금액</span>
                                <span className="fw-semibold fs-5">{cart.totalAmount.toLocaleString()}원</span>
                            </div>
                            <button
                                className="btn btn-primary w-100"
                                onClick={() => navigate('/orders/new', { state: { cartItemIds: cart.cartItems.map((i) => i.cartItemId) } })}
                            >
                                주문하기
                            </button>
                        </div>
                    </div>
                </>
            )}
        </div>
    );
}