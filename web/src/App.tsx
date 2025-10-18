import { BrowserRouter, Routes, Route } from 'react-router-dom';
import MainLayout from './components/MainLayout';
import { AuthProvider } from './contexts/AuthContext'
import RequireAuth from './routes/RequireAuth';
import HomePage from './pages/HomePage';
import CreateTripPage from './pages/CreateTripPage';
import TripsPage from './pages/TripsPage';
import LoginPage from './pages/LoginPage';


export default function App() {
    return (
        <BrowserRouter>
            <AuthProvider>
                <MainLayout>
                    <Routes>
                        <Route path='/' element={<HomePage />} />
                        <Route path='/login' element={<LoginPage />} />
                        <Route path='/trips/new' element={
                            <RequireAuth>
                                <CreateTripPage />
                            </RequireAuth>
                        } />
                        <Route path='/trips' element={
                            <RequireAuth>
                                <TripsPage />
                            </RequireAuth>
                        } />
                    </Routes>
                </MainLayout>
            </AuthProvider>
        </BrowserRouter>
    )
}
