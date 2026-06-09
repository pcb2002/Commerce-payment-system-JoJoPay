import { useState, useEffect } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { previewOrder, createOrder } from '../../api/orderApi';

export default function OrderNewPage() {
    const navigate = useNavigate();
    const location = useLocation();
    const { cartItemIds = [] } = location.state || {};

    const [preview, setPreview] = useState(null);
    const [usedPoint, setUsedPoint] = useState(0);
    const [loading, setLoading] = useState(true);
    const [submitting, setSubmitting] = useState(false);
    const [error, setError] = useState('');

    useEffect(() => {
        if (!cartItemIds.length) {
            setError('주문할 상품이 없습니다. 장바구니를 확인해주세요.');
            setLoading(false);
            return;
        }
        const fetchPreview = async () => {
            try {
                const data = await previewOrder({ cartItemIds });
                setPreview(data.data);
            } catch (err) {
                setError('주문서를 불러오는 데 실패했습니다.');
            } finally {
                setLoading(false);
            }
        };
        fetchPreview();
    }, []);

    const finalAmount = preview ? Math.max(0, preview.totalAmount - usedPoint) : 0;

    const handleSubmit = async () => {
        setSubmitting(true);
        setError('');
        try {
            const data = await createOrder({ cartItemIds, usedPoint });
            navigate(`/orders/${data.data.orderNumber}`, { replace: true });
        } catch (err) {
            setError(err.response?.data?.message || '주문에 실패했습니다.');
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