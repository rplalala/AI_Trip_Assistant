import { BrowserRouter, Routes, Route } from "react-router-dom";
import MainLayout from './components/MainLayout';
import HomePage from "./pages/HomePage";
import PreferencePage from "./pages/PreferencePage";
import LoginPage from "./pages/LoginPage";


export default function App() {
    return (
        <BrowserRouter>
            <MainLayout>
                <Routes>
                    <Route path="/" element={<HomePage />} />
                    <Route path="/preference" element={<PreferencePage />} />
                    <Route path="/login" element={<LoginPage />} />
                </Routes>
            </MainLayout>
        </BrowserRouter>
    )
}
