import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { BrainCircuit, Lock, User, AlertCircle, ShieldCheck } from 'lucide-react';

const Login = () => {
  const [username, setUsername] = useState('admin');
  const [password, setPassword] = useState('admin123');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const success = await login(username, password);
      if (success) {
        navigate('/dashboard');
      } else {
        setError('Invalid administrator credentials. Please check username and password.');
      }
    } catch (err) {
      setError('Connection error. Failed to authenticate with NeuroSys server.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-background flex items-center justify-center p-4 selection:bg-primary-container selection:text-white">
      <div className="w-full max-w-md space-y-6">
        {/* Brand Header */}
        <div className="text-center space-y-2">
          <button 
            onClick={() => navigate('/')}
            className="text-xs font-bold text-primary hover:underline inline-flex items-center gap-1 mb-2 cursor-pointer"
          >
            ← Back to Home
          </button>
          <div className="flex justify-center">
            <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-primary text-white shadow-xl shadow-primary/25 mb-2">
              <BrainCircuit className="w-9 h-9" />
            </div>
          </div>
          <h1 className="font-display text-display font-bold text-on-surface tracking-tight">NeuroSys Admin</h1>
          <p className="font-body-md text-body-md text-secondary">
            Predictive Monitoring & Enterprise Lab Management
          </p>
        </div>

        {/* Login Card */}
        <div className="card-elevated p-8 shadow-lg space-y-6 bg-surface-container-lowest border border-outline-variant">
          <div className="border-b border-outline-variant pb-4">
            <h2 className="font-headline-md text-headline-md font-bold text-on-surface">Sign In to Console</h2>
            <p className="font-body-md text-body-md text-secondary mt-1">Enter your supervisor credentials to access lab assets.</p>
          </div>

          {error && (
            <div className="p-3.5 bg-error-container/40 border border-error/30 rounded-xl text-error text-xs font-bold flex items-center gap-2 animate-fade-in-up">
              <AlertCircle className="w-4 h-4 flex-shrink-0" />
              <span>{error}</span>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="font-label-md text-label-md text-secondary font-bold block mb-1.5">
                Username / Email
              </label>
              <div className="relative">
                <User className="w-4 h-4 text-secondary absolute left-3.5 top-1/2 -translate-y-1/2" />
                <input
                  type="text"
                  required
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  placeholder="admin"
                  className="w-full pl-10 pr-4 py-2.5 bg-surface-container-low border border-outline-variant rounded-lg text-xs font-semibold text-on-surface focus:outline-none focus:border-primary focus:bg-surface-container-lowest transition-all"
                />
              </div>
            </div>

            <div>
              <label className="font-label-md text-label-md text-secondary font-bold block mb-1.5">
                Password
              </label>
              <div className="relative">
                <Lock className="w-4 h-4 text-secondary absolute left-3.5 top-1/2 -translate-y-1/2" />
                <input
                  type="password"
                  required
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••"
                  className="w-full pl-10 pr-4 py-2.5 bg-surface-container-low border border-outline-variant rounded-lg text-xs font-semibold text-on-surface focus:outline-none focus:border-primary focus:bg-surface-container-lowest transition-all"
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full py-3 bg-primary hover:bg-primary-container text-white rounded-xl font-bold text-xs shadow-md shadow-primary/20 transition-all duration-200 active:scale-95 flex items-center justify-center gap-2"
            >
              <ShieldCheck className="w-4 h-4" />
              <span>{loading ? 'Authenticating...' : 'Sign In to Dashboard'}</span>
            </button>
          </form>

          <div className="pt-4 border-t border-outline-variant text-center">
            <p className="text-[11px] font-mono text-secondary">
              Default Credentials: <strong className="text-on-surface">admin / admin123</strong>
            </p>
          </div>
        </div>

        {/* Footer info */}
        <p className="text-center text-xs text-secondary">
          © {new Date().getFullYear()} NeuroSys Systems. All rights reserved.
        </p>
      </div>
    </div>
  );
};

export default Login;
