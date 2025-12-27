import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { useAuthStore } from './stores/authStore';
import Login from './pages/Login';
import Home from './pages/Home';
import Orders from './pages/Orders';
import Monitor from './pages/Monitor';
import Business from './pages/Business';
import PressureTest from './pages/PressureTest';
import FaultInjection from './pages/FaultInjection';

const ProtectedRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { isAuthenticated } = useAuthStore();
  return isAuthenticated ? <>{children}</> : <Navigate to="/" replace />;
};

const App: React.FC = () => {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<Login />} />
        <Route 
          path="/home" 
          element={
            <ProtectedRoute>
              <Home />
            </ProtectedRoute>
          } 
        />
        <Route 
          path="/orders" 
          element={
            <ProtectedRoute>
              <Orders />
            </ProtectedRoute>
          } 
        />
        <Route 
          path="/monitor" 
          element={
            <ProtectedRoute>
              <Monitor />
            </ProtectedRoute>
          } 
        />
        <Route 
          path="/business" 
          element={
            <ProtectedRoute>
              <Business />
            </ProtectedRoute>
          } 
        />
        <Route 
          path="/pressure" 
          element={
            <ProtectedRoute>
              <PressureTest />
            </ProtectedRoute>
          } 
        />
        <Route 
          path="/faults" 
          element={
            <ProtectedRoute>
              <FaultInjection />
            </ProtectedRoute>
          } 
        />
        <Route 
        />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Router>
  );
};

export default App;
