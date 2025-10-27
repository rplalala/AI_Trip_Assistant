import { BrowserRouter, Routes, Route } from 'react-router-dom';
import MainLayout from './components/MainLayout';
import { AuthProvider } from './contexts/AuthContext'
import RequireAuth from './routes/RequireAuth';
import HomePage from './pages/HomePage';
import CreateTripPage from './pages/CreateTripPage';
import TripsPage from './pages/TripsPage';
import TripOverviewPage from './pages/TripOverviewPage';
import LoginPage from './pages/LoginPage';
import UserProfilePage from './pages/UserProfilePage';
import ForgotPasswordPage from './pages/ForgotPasswordPage';
import ResetPasswordPage from './pages/ResetPasswordPage';
import VerifyEmailPage from './pages/VerifyEmailPage';
import VerifyEmailPendingPage from './pages/VerifyEmailPendingPage';
import ChangeEmailPage from './pages/ChangeEmailPage';
import ConfirmChangeEmailPage from './pages/ConfirmChangeEmailPage';


export default function App() {
    return (
        <BrowserRouter>
            <AuthProvider>
                <MainLayout>
                    <Routes>
                        <Route path='/' element={<HomePage />} />
                        <Route path='/login' element={<LoginPage />} />
                        <Route path='/forgot-password' element={<ForgotPasswordPage />} />
                        <Route path='/reset-password' element={<ResetPasswordPage />} />
                        <Route path='/verify-email' element={<VerifyEmailPage />} />
                        <Route path='/verify-email-pending' element={<VerifyEmailPendingPage />} />
                        <Route path='/change-email' element={<ChangeEmailPage />} />
                        <Route path='/confirm-change-email' element={<ConfirmChangeEmailPage />} />
                        <Route path='/profile' element={
                            <RequireAuth>
                                <UserProfilePage />
                            </RequireAuth>
                        } />
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
                        <Route path='/trips/:tripId' element={
                            <RequireAuth>
                                <TripOverviewPage />
                            </RequireAuth>
                        } />
                    </Routes>
                </MainLayout>
            </AuthProvider>
        </BrowserRouter>
    )
}
