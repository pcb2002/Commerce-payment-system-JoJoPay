// src/App.jsx

import { useLocation } from 'react-router-dom';
import Navbar from './components/Navbar';
import Router from './router/Router';

const NO_NAV = ['/login', '/signup'];

export default function App() {
    const location = useLocation();

    return (
        <>
            {!NO_NAV.includes(location.pathname) && <Navbar />}
            <Router />
        </>
    );
}