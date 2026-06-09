import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { getProducts } from '../../api/productApi';

const CATEGORY_OPTIONS = [
    { value: '', label: '전체 카테고리' },
    { value: 'FOOD', label: '식품' },
    { value: 'ELECTRONICS', label: '전자기기' },
    { value: 'CLOTHING', label: '의류' },
    { value: 'BEAUTY', label: '뷰티' },
    { value: 'ETC', label: '기타' },
];

const STATUS_OPTIONS = [
    { value: '', label: '전체 상태' },
    { value: 'ON_SALE', label: '판매중' },
    { value: 'SOLD_OUT', label: '품절' },
    { value: 'DISCONTINUED', label: '판매종료' },
];

const SORT_OPTIONS = [
    { value: 'latest', label: '최신순' },
    { value: 'priceAsc', label: '가격 낮은순' },
    { value: 'priceDesc', label: '가격 높은순' },
];

export default function ProductListPage() {
    const navigate = useNavigate();

    const [products, setProducts] = useState([]);
    const [pageInfo, setPageInfo] = useState({ totalPages: 0, totalElements: 0, number: 0 });
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    const [filters, setFilters] = useState({
        category: '',
        status: '',
        minPrice: '',
        maxPrice: '',
        sort: 'latest',
        page: 0,
        size: 10,
    });

    const fetchProducts = async (params) => {
        setLoading(true);
        setError('');
        try {
            const cleanParams = Object.fromEntries(
                Object.entries(params).filter(([, v]) => v !== '')
            );
            const data = await getProducts(cleanParams);
            setProducts(data.content);
            setPageInfo({ totalPages: data.totalPages, totalElements: data.totalElements, number: data.number });
        } catch (err) {
            setError('상품 목록을 불러오는 데 실패했습니다.');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchProducts(filters);
    }, [filters]);

    const handleFilterChange = (e) => {
        const { name, value } = e.target;
        setFilters((prev) => ({ ...prev, [name]: value, page: 0 }));
    };

    const handlePageChange = (page) => {
        setFilters((prev) => ({ ...prev, page }));
    };

    const handleReset = () => {
        setFilters({ category: '', status: '', minPrice: '', maxPrice: '', sort: 'latest', page: 0, size: 10 });
    };

    const statusBadge = (status) => {
        const map = {
            ON_SALE: { label: '판매중', cls: 'text-bg-success' },
            SOLD_OUT: { label: '품절', cls: 'text-bg-secondary' },
            DISCONTINUED: { label: '판매종료', cls: 'text-bg-danger' },
        };
        const s = map[status] || { label: status, cls: 'text-bg-light' };
        return <span className={`badge ${s.cls}`}>{s.label}</span>;
    };

    return (
        <div className="container py-4">
            <div className="d-flex justify-content-between align-items-center mb-3">
                <h2 className="h5 fw-semibold mb-0">상품 목록</h2>
                <small className="text-muted">총 {pageInfo.totalElements}개</small>
            </div>

            {/* 필터 영역 */}
            <div className="card mb-4">
                <div className="card-body">
                    <div className="row g-2">
                        <div className="col-6 col-md-2">
                            <select name="category" className="form-select form-select-sm" value={filters.category} onChange={handleFilterChange}>
                                {CATEGORY_OPTIONS.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
                            </select>
                        </div>
                        <div className="col-6 col-md-2">
                            <select name="status" className="form-select form-select-sm" value={filters.status} onChange={handleFilterChange}>
                                {STATUS_OPTIONS.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
                            </select>
                        </div>
                        <div className="col-6 col-md-2">
                            <input type="number" name="minPrice" className="form-control form-control-sm" placeholder="최소 가격" value={filters.minPrice} onChange={handleFilterChange} min={0} />
                        </div>
                        <div className="col-6 col-md-2">
                            <input type="number" name="maxPrice" className="form-control form-control-sm" placeholder="최대 가격" value={filters.maxPrice} onChange={handleFilterChange} min={0} />
                        </div>
                        <div className="col-6 col-md-2">
                            <select name="sort" className="form-select form-select-sm" value={filters.sort} onChange={handleFilterChange}>
                                {SORT_OPTIONS.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
                            </select>
                        </div>
                        <div className="col-6 col-md-2">
                            <button className="btn btn-outline-secondary btn-sm w-100" onClick={handleReset}>초기화</button>
                        </div>
                    </div>
                </div>
            </div>

            {/* 에러 */}
            {error && <div className="alert alert-danger">{error}</div>}

            {/* 로딩 */}
            {loading ? (
                <div className="text-center py-5">
                    <div className="spinner-border text-primary" role="status" />
                </div>
            ) : (
                <>
                    {/* 상품 테이블 */}
                    <div className="table-responsive">
                        <table className="table table-hover align-middle">
                            <thead className="table-light">
                            <tr>
                                <th>상품명</th>
                                <th>카테고리</th>
                                <th>가격</th>
                                <th>재고</th>
                                <th>상태</th>
                            </tr>
                            </thead>
                            <tbody>
                            {products.length === 0 ? (
                                <tr>
                                    <td colSpan={5} className="text-center text-muted py-4">상품이 없습니다.</td>
                                </tr>
                            ) : (
                                products.map((p) => (
                                    <tr
                                        key={p.productId}
                                        style={{ cursor: 'pointer' }}
                                        onClick={() => navigate(`/products/${p.productId}`)}
                                    >
                                        <td className="fw-medium">{p.productName}</td>
                                        <td><span className="badge text-bg-light text-dark border">{p.category}</span></td>
                                        <td>{p.price.toLocaleString()}원</td>
                                        <td>{p.stockQuantity}</td>
                                        <td>{statusBadge(p.status)}</td>
                                    </tr>
                                ))
                            )}
                            </tbody>
                        </table>
                    </div>

                    {/* 페이지네이션 */}
                    {pageInfo.totalPages > 1 && (
                        <nav className="d-flex justify-content-center mt-3">
                            <ul className="pagination pagination-sm mb-0">
                                <li className={`page-item ${pageInfo.number === 0 ? 'disabled' : ''}`}>
                                    <button className="page-link" onClick={() => handlePageChange(pageInfo.number - 1)}>이전</button>
                                </li>
                                {Array.from({ length: pageInfo.totalPages }, (_, i) => (
                                    <li key={i} className={`page-item ${pageInfo.number === i ? 'active' : ''}`}>
                                        <button className="page-link" onClick={() => handlePageChange(i)}>{i + 1}</button>
                                    </li>
                                ))}
                                <li className={`page-item ${pageInfo.number === pageInfo.totalPages - 1 ? 'disabled' : ''}`}>
                                    <button className="page-link" onClick={() => handlePageChange(pageInfo.number + 1)}>다음</button>
                                </li>
                            </ul>
                        </nav>
                    )}
                </>
            )}
        </div>
    );
}