import { useState, useRef, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';

export default function Navbar() {
    const navigate = useNavigate();
    const [open, setOpen] = useState(false);
    const dropdownRef = useRef(null);

    const token = localStorage.getItem('jwtToken');
    const userString = localStorage.getItem('user');
    const user = userString ? JSON.parse(userString) : null;
    const username = user?.name || null;

    useEffect(() => {
        const handleClickOutside = (e) => {
            if (dropdownRef.current && !dropdownRef.current.contains(e.target)) {
                setOpen(false);
            }
        };
        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    const handleLogout = () => {
        localStorage.clear();
        navigate('/login');
    };

    const menuItems = [
        { label: '내 정보', path: '/my/profile', icon: 'bi-person' },
        { label: '내 장바구니', path: '/cart', icon: 'bi-cart3' },
        { label: '내 주문', path: '/orders', icon: 'bi-bag' },
        { label: '내 환불', path: '/my/refunds', icon: 'bi-arrow-counterclockwise' },
    ];

    if (!token) return null;

    return (
        <nav className="navbar navbar-expand-lg bg-white border-bottom px-4" style={{ height: 56 }}>
            <Link className="navbar-brand fw-semibold" to="/products">
                jojo-pay
            </Link>

            <div className="ms-auto d-flex align-items-center gap-3" ref={dropdownRef}>
                <div className="position-relative">
                    <button
                        className="btn d-flex align-items-center gap-2 px-2 py-1 rounded-pill border"
                        style={{ fontSize: 14 }}
                        onClick={() => setOpen((prev) => !prev)}
                    >
                        <div
                            className="rounded-circle bg-primary text-white d-flex align-items-center justify-content-center"
                            style={{ width: 30, height: 30, fontSize: 13, fontWeight: 500 }}
                        >
                            {username.slice(0, 1).toUpperCase()}
                        </div>
                        <span className="d-none d-sm-inline">{username}</span>
                        <i className={`bi bi-chevron-${open ? 'up' : 'down'}`} style={{ fontSize: 12 }} />
                    </button>

                    {open && (
                        <ul
                            className="position-absolute end-0 mt-1 bg-white border rounded-3 shadow-sm py-1 list-unstyled"
                            style={{ minWidth: 180, zIndex: 1000, top: '100%' }}
                        >
                            {menuItems.map((item) => (
                                <li key={item.path}>
                                    <Link
                                        className="dropdown-item d-flex align-items-center gap-2 px-3 py-2"
                                        style={{ fontSize: 14 }}
                                        to={item.path}
                                        onClick={() => setOpen(false)}
                                    >
                                        <i className={`bi ${item.icon} text-secondary`} />
                                        {item.label}
                                    </Link>
                                </li>
                            ))}
                            <li><hr className="dropdown-divider my-1" /></li>
                            <li>
                                <button
                                    className="dropdown-item d-flex align-items-center gap-2 px-3 py-2 text-danger"
                                    style={{ fontSize: 14 }}
                                    onClick={handleLogout}
                                >
                                    <i className="bi bi-box-arrow-right" />
                                    로그아웃
                                </button>
                            </li>
                        </ul>
                    )}
                </div>
            </div>
        </nav>
    );
}