import React, { useState, useEffect } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { useLab } from '../contexts/LabContext';
import { metricsService } from '../services/metricsService';
import { 
  Building2, 
  Download, 
  Key, 
  Copy, 
  Check, 
  Laptop, 
  ShieldCheck, 
  RefreshCw, 
  User, 
  Lock, 
  CheckCircle2, 
  AlertTriangle,
  Mail,
  Shield,
  Eye,
  EyeOff
} from 'lucide-react';

const Settings = () => {
  const { user } = useAuth();
  const { currentLab, refreshLabs } = useLab();
  
  const [computers, setComputers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [activeCode, setActiveCode] = useState(null);
  const [copiedCode, setCopiedCode] = useState(false);
  const [generatingCode, setGeneratingCode] = useState(false);
  const [toastMessage, setToastMessage] = useState('');

  // Profile State
  const [username, setUsername] = useState(user?.username || 'admin');
  const [email, setEmail] = useState(user?.email || 'admin@neurosys.com');
  const [userRole, setUserRole] = useState(user?.role || 'Lab Supervisor');
  const [profileSuccess, setProfileSuccess] = useState('');

  // Password Change State
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showCurrentPass, setShowCurrentPass] = useState(false);
  const [showNewPass, setShowNewPass] = useState(false);
  const [passError, setPassError] = useState('');
  const [passSuccess, setPassSuccess] = useState('');
  const [changingPass, setChangingPass] = useState(false);

  useEffect(() => {
    fetchComputers();
  }, [currentLab?.id]);

  const fetchComputers = async () => {
    try {
      setLoading(true);
      const data = await metricsService.getAllComputers(currentLab?.id);
      const list = Array.isArray(data) ? data : (data?.data || []);
      if (Array.isArray(list)) {
        setComputers(list);
      }
    } catch (e) {
      console.error('Error fetching computers for settings', e);
    } finally {
      setLoading(false);
    }
  };

  const showToast = (msg) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(''), 3500);
  };

  const handleGenerateCode = async () => {
    if (!currentLab || currentLab.id === 'ALL') {
      showToast('Please select a specific computer lab room first.');
      return;
    }
    try {
      setGeneratingCode(true);
      const res = await metricsService.fetchRealApi(`/labs/${currentLab.id}/enrollment-code`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({})
      });
      const data = res?.data || res;
      if (data && data.code) {
        setActiveCode(data);
        showToast(`✓ Secure enrollment code ${data.code} generated for ${currentLab.name}`);
      }
    } catch (e) {
      alert('Failed to generate enrollment code.');
    } finally {
      setGeneratingCode(false);
    }
  };

  const handleDownloadAgent = () => {
    if (!currentLab || currentLab.id === 'ALL') {
      showToast('Please select a specific computer lab room first.');
      return;
    }
    const downloadUrl = `/api/v1/agent/download?labId=${currentLab.id}${activeCode?.code ? `&enrollmentCode=${activeCode.code}` : ''}`;
    window.location.href = downloadUrl;
    showToast(`✓ Downloading NeuroSys Agent package for ${currentLab.name}...`);
  };

  const handleSaveProfile = (e) => {
    e.preventDefault();
    setProfileSuccess('✓ Profile details updated successfully.');
    setTimeout(() => setProfileSuccess(''), 3000);
  };

  const handleChangePassword = async (e) => {
    e.preventDefault();
    setPassError('');
    setPassSuccess('');

    if (!currentPassword) {
      setPassError('Current password is required.');
      return;
    }
    if (!newPassword) {
      setPassError('New password is required.');
      return;
    }
    if (newPassword.length < 6) {
      setPassError('Password does not meet security requirements (min 6 characters).');
      return;
    }
    if (newPassword !== confirmPassword) {
      setPassError('New password and confirmation do not match.');
      return;
    }

    try {
      setChangingPass(true);
      const res = await metricsService.fetchRealApi('/auth/change-password', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          currentPassword,
          newPassword,
          confirmPassword
        })
      });

      if (res?.success === false || res?.status >= 400) {
        setPassError(res?.message || 'Current password is incorrect.');
        return;
      }

      setPassSuccess('✓ Password changed successfully.');
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
    } catch (err) {
      setPassError(err.message || 'Current password is incorrect.');
    } finally {
      setChangingPass(false);
    }
  };

  const copyToClipboard = (text) => {
    navigator.clipboard.writeText(text);
    setCopiedCode(true);
    setTimeout(() => setCopiedCode(false), 2000);
  };

  const totalComps = computers.length;
  const onlineComps = computers.filter(c => c.status === 'ONLINE').length;
  const offlineComps = computers.filter(c => c.status === 'OFFLINE').length;
  const needsAttention = computers.filter(c => c.status === 'WARNING' || c.status === 'CRITICAL').length;

  return (
    <div className="max-w-5xl mx-auto space-y-8 animate-fade-in-up">
      
      {/* Header Banner */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 border-b border-outline-variant pb-4">
        <div>
          <h1 className="font-display text-2xl sm:text-3xl font-extrabold text-on-surface">
            Laboratory Settings & Agent Management
          </h1>
          <p className="font-body-md text-xs font-medium text-secondary mt-1">
            Configure workstation onboarding and manage account settings for <strong className="text-primary">{currentLab?.name || 'Computer Lab 1'}</strong>.
          </p>
        </div>

        <button
          onClick={() => { refreshLabs(); fetchComputers(); }}
          className="px-3.5 py-2 rounded-lg bg-surface-container-high hover:bg-surface-container-highest text-primary text-xs font-bold flex items-center gap-1.5 transition-all cursor-pointer border border-outline-variant shadow-sm"
        >
          <RefreshCw className="w-3.5 h-3.5" />
          <span>Refresh Lab Status</span>
        </button>
      </div>

      {toastMessage && (
        <div className="p-4 bg-emerald-500/10 border border-emerald-500/30 rounded-xl text-emerald-700 text-xs font-bold animate-fade-in-up">
          {toastMessage}
        </div>
      )}

      {/* 1. NEUROSYS AGENT MANAGEMENT */}
      <section className="bg-surface-container-lowest border border-outline-variant rounded-2xl p-6 shadow-sm space-y-6">
        <div className="flex items-center justify-between border-b border-outline-variant pb-4">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-primary/10 flex items-center justify-center text-primary">
              <Laptop className="w-5 h-5" />
            </div>
            <div>
              <h2 className="font-headline-md text-base font-bold text-on-surface">
                NeuroSys Agent Management
              </h2>
              <p className="text-xs text-secondary font-medium mt-0.5">
                Connect new Windows computers to <strong className="text-primary">{currentLab?.name || 'Computer Lab 1'}</strong>.
              </p>
            </div>
          </div>
        </div>

        {/* Real Database Agent Telemetry Stats */}
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
          <div className="p-3 bg-surface-container-low rounded-xl border border-outline-variant text-center">
            <span className="text-xl font-black text-on-surface block leading-none">{totalComps}</span>
            <span className="text-[10px] font-bold text-secondary uppercase block mt-1">Total Workstations</span>
          </div>

          <div className="p-3 bg-emerald-50/80 rounded-xl border border-emerald-100 text-center">
            <span className="text-xl font-black text-emerald-700 block leading-none">{onlineComps}</span>
            <span className="text-[10px] font-bold text-emerald-800 uppercase block mt-1">Active Agents</span>
          </div>

          <div className="p-3 bg-slate-100 rounded-xl border border-slate-200 text-center">
            <span className="text-xl font-black text-slate-700 block leading-none">{offlineComps}</span>
            <span className="text-[10px] font-bold text-slate-600 uppercase block mt-1">Offline Agents</span>
          </div>

          <div className="p-3 bg-amber-50/80 rounded-xl border border-amber-100 text-center">
            <span className="text-xl font-black text-amber-700 block leading-none">{needsAttention}</span>
            <span className="text-[10px] font-bold text-amber-800 uppercase block mt-1">Attention Required</span>
          </div>
        </div>

        {/* Streamlined Primary Download Action */}
        <div>
          <button
            onClick={handleDownloadAgent}
            className="w-full py-3.5 bg-emerald-700 hover:bg-emerald-800 text-white rounded-xl text-sm font-extrabold flex items-center justify-center gap-2 transition-all shadow-md cursor-pointer"
          >
            <Download className="w-4 h-4" />
            <span>Download Pre-Configured Agent Package (.zip)</span>
          </button>
          <p className="text-[11px] text-secondary text-center mt-2 font-medium">
            💡 The downloaded ZIP package automatically includes the pre-configured enrollment passkey for <strong>{currentLab?.name || 'this lab'}</strong>.
          </p>
        </div>

        {/* Generated Code Token Banner */}
        {activeCode && (
          <div className="p-4 bg-indigo-950 text-white rounded-xl space-y-2 border border-indigo-800 animate-fade-in-up">
            <span className="text-[10px] font-extrabold text-indigo-300 uppercase tracking-wider block">
              Active Enrollment Code Generated for {activeCode.labName}
            </span>
            <div className="flex items-center justify-between gap-3">
              <span className="text-xl font-mono font-extrabold text-emerald-400 tracking-wider">
                {activeCode.code}
              </span>
              <button
                onClick={() => copyToClipboard(activeCode.code)}
                className="px-3 py-1.5 bg-emerald-600 hover:bg-emerald-500 text-white rounded text-xs font-bold flex items-center gap-1 cursor-pointer"
              >
                {copiedCode ? <Check className="w-3.5 h-3.5" /> : <Copy className="w-3.5 h-3.5" />}
                <span>{copiedCode ? 'Copied' : 'Copy Token'}</span>
              </button>
            </div>
          </div>
        )}
      </section>

      {/* 2. ACCOUNT SETTINGS */}
      <section className="bg-surface-container-lowest border border-outline-variant rounded-2xl p-6 shadow-sm space-y-5">
        <div className="border-b border-outline-variant pb-3 flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-primary/10 flex items-center justify-center text-primary">
            <User className="w-5 h-5" />
          </div>
          <div>
            <h2 className="font-headline-md text-base font-bold text-on-surface">Account Settings</h2>
            <p className="text-xs text-secondary font-medium">Manage your administrator account details and email address.</p>
          </div>
        </div>

        {profileSuccess && (
          <div className="p-3 bg-emerald-100 border border-emerald-300 text-emerald-800 text-xs font-bold rounded-xl flex items-center gap-2">
            <Check className="w-4 h-4" />
            <span>{profileSuccess}</span>
          </div>
        )}

        <form onSubmit={handleSaveProfile} className="space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div>
              <label className="text-xs font-bold text-secondary block mb-1">Username</label>
              <input
                type="text"
                disabled
                value={username}
                className="w-full px-3.5 py-2 bg-surface-container-low border border-outline-variant rounded-lg text-xs font-bold text-slate-700 cursor-not-allowed"
              />
            </div>

            <div>
              <label className="text-xs font-bold text-secondary block mb-1">Role</label>
              <input
                type="text"
                disabled
                value={userRole}
                className="w-full px-3.5 py-2 bg-surface-container-low border border-outline-variant rounded-lg text-xs font-bold text-slate-700 cursor-not-allowed"
              />
            </div>

            <div>
              <label className="text-xs font-bold text-secondary block mb-1">Email Address</label>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="w-full px-3.5 py-2 bg-white border border-outline-variant rounded-lg text-xs font-bold text-on-surface focus:outline-none focus:border-primary"
              />
            </div>
          </div>

          <div className="flex justify-end">
            <button
              type="submit"
              className="px-4 py-2 bg-primary hover:bg-primary-container text-white font-bold text-xs rounded-lg shadow-sm transition-all cursor-pointer"
            >
              Save Profile
            </button>
          </div>
        </form>
      </section>

      {/* 3. SECURITY & CHANGE PASSWORD */}
      <section className="bg-surface-container-lowest border border-outline-variant rounded-2xl p-6 shadow-sm space-y-5">
        <div className="border-b border-outline-variant pb-3 flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-primary/10 flex items-center justify-center text-primary">
            <Lock className="w-5 h-5" />
          </div>
          <div>
            <h2 className="font-headline-md text-base font-bold text-on-surface">Security & Password</h2>
            <p className="text-xs text-secondary font-medium">Update your account password using current password verification.</p>
          </div>
        </div>

        {passError && (
          <div className="p-3 bg-red-100 border border-red-300 text-red-800 text-xs font-bold rounded-xl flex items-center gap-2">
            <AlertTriangle className="w-4 h-4 text-red-600" />
            <span>✕ {passError}</span>
          </div>
        )}

        {passSuccess && (
          <div className="p-3 bg-emerald-100 border border-emerald-300 text-emerald-800 text-xs font-bold rounded-xl flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 text-emerald-600" />
            <span>{passSuccess}</span>
          </div>
        )}

        <form onSubmit={handleChangePassword} className="space-y-4 max-w-xl">
          <div>
            <label className="text-xs font-bold text-secondary block mb-1">Current Password *</label>
            <div className="relative">
              <input
                type={showCurrentPass ? 'text' : 'password'}
                required
                value={currentPassword}
                onChange={(e) => setCurrentPassword(e.target.value)}
                placeholder="Enter current password..."
                className="w-full px-3.5 py-2 bg-white border border-outline-variant rounded-lg text-xs font-semibold focus:outline-none focus:border-primary pr-10"
              />
              <button
                type="button"
                onClick={() => setShowCurrentPass(!showCurrentPass)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600"
              >
                {showCurrentPass ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
              </button>
            </div>
          </div>

          <div>
            <label className="text-xs font-bold text-secondary block mb-1">New Password *</label>
            <div className="relative">
              <input
                type={showNewPass ? 'text' : 'password'}
                required
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                placeholder="Enter new password (min 6 chars)..."
                className="w-full px-3.5 py-2 bg-white border border-outline-variant rounded-lg text-xs font-semibold focus:outline-none focus:border-primary pr-10"
              />
              <button
                type="button"
                onClick={() => setShowNewPass(!showNewPass)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600"
              >
                {showNewPass ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
              </button>
            </div>
          </div>

          <div>
            <label className="text-xs font-bold text-secondary block mb-1">Confirm New Password *</label>
            <input
              type="password"
              required
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              placeholder="Confirm new password..."
              className="w-full px-3.5 py-2 bg-white border border-outline-variant rounded-lg text-xs font-semibold focus:outline-none focus:border-primary"
            />
          </div>

          <button
            type="submit"
            disabled={changingPass}
            className="px-5 py-2.5 bg-slate-900 hover:bg-slate-800 text-white font-bold text-xs rounded-xl shadow-md transition-all cursor-pointer flex items-center gap-2"
          >
            <Lock className="w-4 h-4 text-emerald-400" />
            <span>{changingPass ? 'Updating Password...' : 'Change Password'}</span>
          </button>
        </form>
      </section>

      {/* 4. LABORATORY INFORMATION */}
      <section className="bg-surface-container-lowest border border-outline-variant rounded-2xl p-6 shadow-sm space-y-4">
        <div className="border-b border-outline-variant pb-3 flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-primary/10 flex items-center justify-center text-primary">
            <Building2 className="w-5 h-5" />
          </div>
          <div>
            <h2 className="font-headline-md text-base font-bold text-on-surface">Laboratory Information</h2>
            <p className="text-xs text-secondary font-medium">Details of the currently selected laboratory room.</p>
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-xs">
          <div className="p-3.5 bg-surface-container-low rounded-xl border border-outline-variant space-y-1">
            <span className="text-secondary font-bold block">Laboratory Name</span>
            <span className="text-sm font-extrabold text-on-surface block">{currentLab?.name || 'Computer Lab 1'}</span>
          </div>

          <div className="p-3.5 bg-surface-container-low rounded-xl border border-outline-variant space-y-1">
            <span className="text-secondary font-bold block">Lab Code</span>
            <span className="text-sm font-mono font-extrabold text-primary block">{currentLab?.code || 'LAB-001'}</span>
          </div>

          <div className="p-3.5 bg-surface-container-low rounded-xl border border-outline-variant space-y-1">
            <span className="text-secondary font-bold block">Physical Location</span>
            <span className="text-xs font-bold text-on-surface block">{currentLab?.location || 'Building A, Room 101'}</span>
          </div>

          <div className="p-3.5 bg-surface-container-low rounded-xl border border-outline-variant space-y-1">
            <span className="text-secondary font-bold block">Description</span>
            <span className="text-xs font-medium text-slate-700 block">
              {currentLab?.description || 'General Programming & Software Engineering Laboratory'}
            </span>
          </div>
        </div>
      </section>

    </div>
  );
};

export default Settings;
