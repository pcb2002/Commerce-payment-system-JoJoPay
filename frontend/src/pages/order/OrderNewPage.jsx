import { useState, useEffect } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { previewOrder, createOrder } from '../../api/orderApi';
import { confirmPayment } from '../../api/paymentApi';

export default function OrderNewPage() {
    const navigate = useNavigate();
    const location = useLocation();
    const { cartItemIds, productId, quantity } = location.state || {};

    const [preview, setPreview] = useState(null);
    const [usedPoint, setUsedPoint] = useState(0);
    const [loading, setLoading] = useState(true);
    const [submitting, setSubmitting] = useState(false);
    const [error, setError] = useState('');

    useEffect(() => {
        // 둘 다 없으면 에러
        if (!cartItemIds && (!productId || !quantity)) {
            setError('주문할 상품 정보를 불러올 수 없습니다.');
            setLoading(false);
            return;
        }

        const fetchPreview = async () => {
            try {
                // API에 넘겨줄 데이터 구성
                const requestData = cartItemIds
                    ? { cartItemIds }
                    : { productId, quantity };

                const data = await previewOrder(requestData);
                setPreview(data.data);
            } catch (err) {
                setError('주문서를 불러오는 데 실패했습니다.');
            } finally {
                setLoading(false);
            }
        };
        fetchPreview();
    }, [cartItemIds, productId, quantity]);

    const finalAmount = preview ? Math.max(0, preview.totalAmount - usedPoint) : 0;

    const handleSubmit = async () => {
        setSubmitting(true);
        setError('');

        try {
            const userString = localStorage.getItem('user');
            const user = userString ? JSON.parse(userString) : null;

            // 1. 주문 생성 (백엔드에서 orderNumber와 portonePaymentId를 반환한다고 가정)
            // 주의: 백엔드 createOrder API가 portonePaymentId를 리턴하도록 수정되어 있어야 합니다!
            const orderRequestData = cartItemIds
                ? { cartItemIds, usedPoint }
                : { productId, quantity, usedPoint };

            const orderData = await createOrder(orderRequestData);
            const { orderNumber, portonePaymentId } = orderData.data;

            const portone = window.PortOne;
            if (!portone) {
                throw new Error('결제 모듈을 불러올 수 없습니다.');
            }

            // 2. 포트원 결제창 띄우기 (Promise 방식 사용)
            const response = await portone.requestPayment({
                storeId: import.meta.env.VITE_PORTONE_STORE_ID,
                channelKey: import.meta.env.VITE_PORTONE_CHANNEL_KEY,
                paymentId: portonePaymentId, // ★중요: 백엔드에서 생성된 ID를 그대로 사용
                orderName: preview.items[0].productName + (preview.items.length > 1 ? ` 외 ${preview.items.length - 1}건` : ''),
                totalAmount: finalAmount,
                currency: 'KRW',
                payMethod: 'CARD',
                // redirectUrl 제거: Promise로 결과 받아서 백엔드 검증을 거친 후 페이지 이동할 것이므로 불필요
                customer: {
                    fullName: user?.name || null,
                    email: user?.email || null,
                    phoneNumber: user?.phoneNumber || null
                }
            });

            // 3. 결제창에서 에러 발생 시 처리
            if (response.code !== undefined) {
                setError(`결제 실패: ${response.message}`);
                setSubmitting(false);
                return;
            }

            // 4. 결제 성공 시 백엔드에 '결제 완료되었으니 검증해줘'라고 요청
            // 이 과정이 있어야 백엔드에서 포인트 차감/적립/주문상태 변경이 일어납니다.
            await confirmPayment({
                orderId: orderNumber, // 또는 백엔드 요구사항에 맞게
                portonePaymentId: portonePaymentId
            });

            // 5. 검증 성공 시 이동
            alert("결제가 완료되었습니다.");
            navigate(`/orders/${orderNumber}`);

        } catch (err) {
            console.error(err);
            setError(err.response?.data?.message || err.message || '결제 중 오류가 발생했습니다.');
            setSubmitting(false);
        }
    };

    if (loading) return (
        <div className="d-flex justify-content-center align-items-center" style={{ minHeight: '50vh' }}>
            <div className="spinner-border text-primary" role="status" />
        </div>
    );

    if (error && !preview) return (
        <div className="container py-4">
            <div className="alert alert-danger">{error}</div>
            <button className="btn btn-outline-secondary btn-sm" onClick={() => navigate('/cart')}>← 장바구니로</button>
        </div>
    );

    return (
        <div className="container py-4" style={{ maxWidth: '640px' }}>
            <button className="btn btn-outline-secondary btn-sm mb-3" onClick={() => navigate(-1)}>← 뒤로가기</button>
            <h2 className="h5 fw-semibold mb-4">주문서</h2>

            <div className="card mb-3">
                <div className="card-header bg-white fw-medium">주문 상품</div>
                <ul className="list-group list-group-flush">
                    {preview?.items.map((item) => (
                        <li key={item.productId} className="list-group-item">
                            <div className="d-flex justify-content-between align-items-center">
                                <div>
                                    <p className="fw-medium mb-0">{item.productName}</p>
                                    <small className="text-muted">{item.price.toLocaleString()}원 × {item.quantity}</small>
                                </div>
                                <span className="fw-medium">{(item.price * item.quantity).toLocaleString()}원</span>
                            </div>
                        </li>
                    ))}
                </ul>
            </div>

            <div className="card mb-3">
                <div className="card-body">
                    <label className="form-label fw-medium">포인트 사용</label>
                    <div className="input-group">
                        <input
                            type="number"
                            className="form-control"
                            min={0}
                            max={preview?.totalAmount || 0}
                            value={usedPoint}
                            onChange={(e) => setUsedPoint(Math.max(0, Number(e.target.value)))}
                            placeholder="사용할 포인트 입력"
                        />
                        <span className="input-group-text">P</span>
                    </div>
                    <div className="form-text">최대 {preview?.totalAmount.toLocaleString() || 0}P 사용 가능</div>
                </div>
            </div>

            <div className="card mb-4">
                <div className="card-body">
                    <div className="d-flex justify-content-between mb-2 text-muted small">
                        <span>상품 금액</span>
                        <span>{preview?.totalAmount.toLocaleString()}원</span>
                    </div>
                    <div className="d-flex justify-content-between mb-2 text-muted small">
                        <span>포인트 할인</span>
                        <span className="text-danger">- {usedPoint.toLocaleString()}P</span>
                    </div>
                    <hr className="my-2" />
                    <div className="d-flex justify-content-between fw-semibold fs-5">
                        <span>최종 결제 금액</span>
                        <span>{finalAmount.toLocaleString()}원</span>
                    </div>
                </div>
            </div>

            {error && <div className="alert alert-danger py-2 small mb-3">{error}</div>}

            <button className="btn btn-primary w-100" onClick={handleSubmit} disabled={submitting}>
                {submitting ? <span className="spinner-border spinner-border-sm me-2" /> : null}
                {finalAmount.toLocaleString()}원 결제하기
            </button>
        </div>
    );
}