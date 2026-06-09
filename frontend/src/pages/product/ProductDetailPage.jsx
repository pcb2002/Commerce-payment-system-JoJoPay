import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getProductDetail } from '../../api/productApi';
import { addCartItem } from '../../api/cartApi';

export default function ProductDetailPage() {
    const { productId } = useParams();
    const navigate = useNavigate();

    const [product, setProduct] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [quantity, setQuantity] = useState(1);
    const [cartLoading, setCartLoading] = useState(false);
    const [cartSuccess, setCartSuccess] = useState(false);

    const handleAddCart = async () => {
        setCartLoading(true);
        setCartSuccess(false);
        try {
            await addCartItem({ productId: product.productId, quantity });
            setCartSuccess(true);
            setTimeout(() => setCartSuccess(false), 2000);
        } catch (err) {
            alert('장바구니 담기에 실패했습니다.');
        } finally {
            setCartLoading(false);
        }
    };

    useEffect(() => {
        const fetch = async () => {
            try {
                const data = await getProductDetail(productId);
                setProduct(data);
            } catch (err) {
                setError('상품 정보를 불러오는 데 실패했습니다.');
            } finally {
                setLoading(false);
            }
        };
        fetch();
    }, [productId]);

    const statusBadge = (status) => {
        const map = {
            ON_SALE: { label: '판매중', cls: 'text-bg-success' },
            SOLD_OUT: { label: '품절', cls: 'text-bg-secondary' },
            DISCONTINUED: { label: '판매종료', cls: 'text-bg-danger' },
        };
        const s = map[status] || { label: status, cls: 'text-bg-light' };
        return <span className={`badge ${s.cls}`}>{s.label}</span>;
    };

    if (loading) return (
        <div className="d-flex justify-content-center align-items-center" style={{ minHeight: '50vh' }}>
            <div className="spinner-border text-primary" role="status" />
        </div>
    );

    if (error) return (
        <div className="container py-4">
            <div className="alert alert-danger">{error}</div>
            <button className="btn btn-outline-secondary btn-sm" onClick={() => navigate(-1)}>← 뒤로가기</button>
        </div>
    );

    return (
        <div className="container py-4" style={{ maxWidth: '640px' }}>
            <button className="btn btn-outline-secondary btn-sm mb-3" onClick={() => navigate(-1)}>← 목록으로</button>

            <div className="card">
                <div className="card-body p-4">
                    <div className="d-flex justify-content-between align-items-start mb-3">
                        <h1 className="h4 fw-semibold mb-0">{product.productName}</h1>
                        {statusBadge(product.status)}
                    </div>

                    <div className="mb-3">
                        <span className="badge text-bg-light text-dark border">{product.category}</span>
                    </div>

                    <hr />

                    <table className="table table-borderless mb-0">
                        <tbody>
                        <tr>
                            <td className="text-muted ps-0" style={{ width: '120px' }}>가격</td>
                            <td className="fw-semibold fs-5">{product.price.toLocaleString()}원</td>
                        </tr>
                        <tr>
                            <td className="text-muted ps-0">재고</td>
                            <td>{product.stockQuantity}개</td>
                        </tr>
                        <tr>
                            <td className="text-muted ps-0">상품 설명</td>
                            <td style={{ whiteSpace: 'pre-wrap' }}>{product.description || '-'}</td>
                        </tr>
                        <tr>
                            <td className="text-muted ps-0">등록일</td>
                            <td>{new Date(product.createdAt).toLocaleDateString('ko-KR')}</td>
                        </tr>
                        <tr>
                            <td className="text-muted ps-0">수정일</td>
                            <td>{new Date(product.updatedAt).toLocaleDateString('ko-KR')}</td>
                        </tr>
                        </tbody>
                    </table>

                    <hr className="mb-3" />

                    {/* 수량 선택 */}
                    <div className="d-flex align-items-center gap-2 mb-3">
                        <span className="text-muted small">수량</span>
                        <button
                            className="btn btn-outline-secondary btn-sm"
                            style={{ width: '32px', padding: '0' }}
                            disabled={quantity <= 1}
                            onClick={() => setQuantity((q) => q - 1)}
                        >−</button>
                        <span style={{ minWidth: '28px', textAlign: 'center' }}>{quantity}</span>
                        <button
                            className="btn btn-outline-secondary btn-sm"
                            style={{ width: '32px', padding: '0' }}
                            disabled={quantity >= product.stockQuantity}
                            onClick={() => setQuantity((q) => q + 1)}
                        >+</button>
                    </div>

                    <div className="d-flex gap-2">
                        <button
                            className={`btn flex-grow-1 ${cartSuccess ? 'btn-success' : 'btn-primary'}`}
                            disabled={product.status !== 'ON_SALE' || cartLoading}
                            onClick={handleAddCart}
                        >
                            {cartLoading
                                ? <span className="spinner-border spinner-border-sm me-1" />
                                : cartSuccess ? '✓ 담겼습니다!' : '장바구니 담기'}
                        </button>
                        <button
                            className="btn btn-outline-primary flex-grow-1"
                            disabled={product.status !== 'ON_SALE'}
                            onClick={() => navigate('/orders/new', { state: { productId: product.productId, quantity } })}
                        >
                            바로 주문
                        </button>
                    </div>
                    {cartSuccess && (
                        <div className="mt-2 text-end">
                            <button className="btn btn-link btn-sm p-0 text-decoration-none" onClick={() => navigate('/cart')}>
                                장바구니 보기 →
                            </button>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}