import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { 
  BrainCircuit, 
  Activity, 
  AlertTriangle, 
  Brain, 
  TrendingUp, 
  Power, 
  CheckCircle2, 
  PackageCheck, 
  Building2, 
  ArrowRight, 
  Lock, 
  RotateCcw, 
  ShieldCheck, 
  Terminal, 
  Users, 
  ChevronRight, 
  Menu, 
  X,
  Laptop,
  Check,
  Server
} from 'lucide-react';

const LandingPage = () => {
  const navigate = useNavigate();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const scrollToSection = (id) => {
    setMobileMenuOpen(false);
    const element = document.getElementById(id);
    if (element) {
      element.scrollIntoView({ behavior: 'smooth' });
    }
  };

  return (
    <div className="min-h-screen bg-background text-on-surface font-body-md selection:bg-primary/20 selection:text-primary">
      
      {/* 1. Header / Navigation */}
      <header className="sticky top-0 z-50 bg-white/90 backdrop-blur-md border-b border-slate-200">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
          
          {/* Logo / Brand Name */}
          <div 
            onClick={() => window.scrollTo({ top: 0, behavior: 'smooth' })}
            className="flex items-center gap-2.5 cursor-pointer group"
          >
            <div className="w-10 h-10 rounded-xl bg-primary text-white flex items-center justify-center shadow-md shadow-primary/20 group-hover:scale-105 transition-transform">
              <BrainCircuit className="w-6 h-6" />
            </div>
            <div>
              <span className="font-display text-xl font-black text-slate-900 tracking-tight block leading-none">
                Neuro<span className="text-primary">Sys</span>
              </span>
              <span className="text-[10px] font-bold text-slate-500 tracking-wider uppercase block mt-0.5">
                Lab Platform
              </span>
            </div>
          </div>

          {/* Desktop Navigation Links */}
          <nav className="hidden md:flex items-center gap-6 text-xs font-bold text-slate-700">
            <button onClick={() => scrollToSection('hero')} className="hover:text-primary transition-colors cursor-pointer">Home</button>
            <button onClick={() => scrollToSection('features')} className="hover:text-primary transition-colors cursor-pointer">Features</button>
            <button onClick={() => scrollToSection('how-it-works')} className="hover:text-primary transition-colors cursor-pointer">How It Works</button>
            <button onClick={() => scrollToSection('centralized')} className="hover:text-primary transition-colors cursor-pointer">Centralized</button>
            <button onClick={() => scrollToSection('ai-intelligence')} className="hover:text-primary transition-colors cursor-pointer">AI Intelligence</button>
            <button onClick={() => scrollToSection('security')} className="hover:text-primary transition-colors cursor-pointer">Security</button>
            <button onClick={() => scrollToSection('about')} className="hover:text-primary transition-colors cursor-pointer">About</button>
          </nav>

          {/* Right Action Button */}
          <div className="hidden md:flex items-center gap-3">
            <button
              onClick={() => navigate('/login')}
              className="px-5 py-2.5 rounded-lg bg-primary hover:bg-primary-container text-white text-xs font-bold transition-all cursor-pointer shadow-md shadow-primary/20 hover:shadow-lg flex items-center gap-2"
            >
              <span>Sign In</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </button>
          </div>

          {/* Mobile Menu Button */}
          <button
            onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
            className="md:hidden p-2 text-slate-700 hover:bg-slate-100 rounded-lg cursor-pointer"
          >
            {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
          </button>
        </div>

        {/* Mobile Dropdown Menu */}
        {mobileMenuOpen && (
          <div className="md:hidden bg-white border-b border-slate-200 px-4 py-4 space-y-3 shadow-xl">
            <button onClick={() => scrollToSection('hero')} className="block w-full text-left py-2 text-sm font-bold text-slate-800 hover:text-primary">Home</button>
            <button onClick={() => scrollToSection('features')} className="block w-full text-left py-2 text-sm font-bold text-slate-800 hover:text-primary">Features</button>
            <button onClick={() => scrollToSection('how-it-works')} className="block w-full text-left py-2 text-sm font-bold text-slate-800 hover:text-primary">How It Works</button>
            <button onClick={() => scrollToSection('centralized')} className="block w-full text-left py-2 text-sm font-bold text-slate-800 hover:text-primary">Centralized</button>
            <button onClick={() => scrollToSection('ai-intelligence')} className="block w-full text-left py-2 text-sm font-bold text-slate-800 hover:text-primary">AI Intelligence</button>
            <button onClick={() => scrollToSection('security')} className="block w-full text-left py-2 text-sm font-bold text-slate-800 hover:text-primary">Security</button>
            <button onClick={() => scrollToSection('about')} className="block w-full text-left py-2 text-sm font-bold text-slate-800 hover:text-primary">About</button>
            <button
              onClick={() => navigate('/login')}
              className="w-full mt-2 py-3 bg-primary text-white text-xs font-bold rounded-lg text-center shadow-md"
            >
              Sign In to NeuroSys
            </button>
          </div>
        )}
      </header>

      {/* 2. Hero Section */}
      <section id="hero" className="relative py-16 lg:py-24 overflow-hidden bg-gradient-to-b from-white via-surface-container-low to-background">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-12 items-center">
            
            <div className="lg:col-span-7 space-y-6">
              <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-primary/10 border border-primary/20 text-primary text-xs font-extrabold uppercase tracking-wider">
                <BrainCircuit className="w-3.5 h-3.5" />
                <span>AI-Powered Computer Lab Platform for Colleges</span>
              </div>

              <h1 className="font-display text-4xl sm:text-5xl lg:text-6xl font-black text-slate-900 tracking-tight leading-[1.1]">
                Smarter Computer Lab Management
              </h1>

              <p className="text-base sm:text-lg text-slate-700 font-medium max-w-2xl leading-relaxed">
                Monitor, manage, and maintain your college computers from one centralized platform.
              </p>

              <div className="flex flex-wrap items-center gap-4 pt-2">
                <button
                  onClick={() => navigate('/login')}
                  className="px-6 py-3.5 rounded-xl bg-primary hover:bg-primary-container text-white text-sm font-extrabold transition-all cursor-pointer shadow-lg shadow-primary/25 hover:shadow-xl flex items-center gap-2"
                >
                  <span>Get Started</span>
                  <ArrowRight className="w-4 h-4" />
                </button>
                <button
                  onClick={() => scrollToSection('features')}
                  className="px-6 py-3.5 rounded-xl bg-white hover:bg-slate-100 border border-slate-300 text-slate-900 text-sm font-bold transition-all cursor-pointer shadow-sm"
                >
                  Explore Features
                </button>
              </div>

              <div className="pt-6 grid grid-cols-3 gap-4 border-t border-slate-200 text-left">
                <div>
                  <div className="text-xl font-extrabold text-slate-900 font-display">1-Second</div>
                  <div className="text-xs text-slate-600 font-medium mt-0.5">Real-Time Status</div>
                </div>
                <div>
                  <div className="text-xl font-extrabold text-slate-900 font-display">Centralized</div>
                  <div className="text-xs text-slate-600 font-medium mt-0.5">Multi-Lab Control</div>
                </div>
                <div>
                  <div className="text-xl font-extrabold text-slate-900 font-display">AI-Driven</div>
                  <div className="text-xs text-slate-600 font-medium mt-0.5">Crash Risk Prediction</div>
                </div>
              </div>
            </div>

            {/* Hero Graphic Card (Conceptual Mock, No Private Data) */}
            <div className="lg:col-span-5">
              <div className="bg-slate-900 text-white rounded-2xl p-6 shadow-2xl border border-slate-800 space-y-5 relative overflow-hidden">
                <div className="flex items-center justify-between border-b border-slate-800 pb-4">
                  <div className="flex items-center gap-2">
                    <div className="w-3 h-3 rounded-full bg-emerald-500 animate-pulse"></div>
                    <span className="text-xs font-bold text-slate-200">NeuroSys Platform Overview</span>
                  </div>
                  <span className="text-[10px] font-mono font-bold bg-primary/20 text-indigo-300 px-2 py-0.5 rounded border border-primary/30">
                    Live Cloud Platform
                  </span>
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <div className="p-3 bg-slate-800/80 rounded-xl border border-slate-700/60">
                    <span className="text-[11px] text-slate-400 font-medium block">Fleet Monitoring</span>
                    <span className="text-sm font-extrabold text-emerald-400 mt-1 block">Active 1s Heartbeat</span>
                  </div>
                  <div className="p-3 bg-slate-800/80 rounded-xl border border-slate-700/60">
                    <span className="text-[11px] text-slate-400 font-medium block">Lab Readiness</span>
                    <span className="text-sm font-extrabold text-indigo-300 mt-1 block">Practical Verified</span>
                  </div>
                </div>

                <div className="p-4 bg-slate-800/60 rounded-xl border border-slate-700/50 space-y-2">
                  <div className="flex items-center justify-between text-xs font-bold text-slate-300">
                    <span>Centralized Control Panel</span>
                    <span className="text-emerald-400 text-[11px]">System Ready</span>
                  </div>
                  <div className="w-full bg-slate-700 h-2 rounded-full overflow-hidden">
                    <div className="bg-primary h-full rounded-full w-4/5"></div>
                  </div>
                </div>

                <div className="flex items-center justify-between text-[11px] text-slate-400 pt-1 border-t border-slate-800/80">
                  <span>Role-Based Auth</span>
                  <span>Remote Power Management</span>
                </div>
              </div>
            </div>

          </div>
        </div>
      </section>

      {/* 3. Product Description Section */}
      <section className="py-12 bg-white border-y border-slate-200">
        <div className="max-w-5xl mx-auto px-4 text-center space-y-3">
          <h2 className="text-xs font-extrabold uppercase tracking-widest text-primary">Purpose-Built Platform</h2>
          <p className="text-lg sm:text-xl font-medium text-slate-800 leading-relaxed max-w-4xl mx-auto">
            NeuroSys is an AI-powered computer lab management platform designed for colleges. It helps lab administrators monitor computers, identify persistent performance problems, check software readiness, predict potential issues, and remotely manage systems from one place.
          </p>
        </div>
      </section>

      {/* 4. Why NeuroSys? (Feature Grid - 8 Cards) */}
      <section id="features" className="py-16 sm:py-24 bg-surface-container-low">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 space-y-12">
          
          <div className="text-center space-y-3 max-w-3xl mx-auto">
            <h2 className="text-xs font-extrabold uppercase tracking-widest text-primary">Comprehensive Features</h2>
            <h3 className="font-display text-3xl sm:text-4xl font-extrabold text-slate-900 tracking-tight">
              Everything your computer lab needs in one place.
            </h3>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
            
            {/* Card 1 */}
            <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm hover:shadow-md transition-all space-y-3">
              <div className="w-12 h-12 rounded-xl bg-indigo-100 text-primary flex items-center justify-center">
                <Activity className="w-6 h-6" />
              </div>
              <h4 className="text-base font-extrabold text-slate-900">1. Real-Time Monitoring</h4>
              <p className="text-xs text-slate-600 leading-relaxed font-medium">
                Monitor CPU, memory, disk, network, and system health across your workstation fleet.
              </p>
            </div>

            {/* Card 2 */}
            <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm hover:shadow-md transition-all space-y-3">
              <div className="w-12 h-12 rounded-xl bg-amber-100 text-amber-700 flex items-center justify-center">
                <AlertTriangle className="w-6 h-6" />
              </div>
              <h4 className="text-base font-extrabold text-slate-900">2. Intelligent Alerts</h4>
              <p className="text-xs text-slate-600 leading-relaxed font-medium">
                Identify persistent problems instead of notifying administrators about every temporary spike.
              </p>
            </div>

            {/* Card 3 */}
            <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm hover:shadow-md transition-all space-y-3">
              <div className="w-12 h-12 rounded-xl bg-purple-100 text-purple-700 flex items-center justify-center">
                <Brain className="w-6 h-6" />
              </div>
              <h4 className="text-base font-extrabold text-slate-900">3. AI Diagnosis</h4>
              <p className="text-xs text-slate-600 leading-relaxed font-medium">
                Help administrators understand what may be causing a computer's performance problem using available system evidence.
              </p>
            </div>

            {/* Card 4 */}
            <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm hover:shadow-md transition-all space-y-3">
              <div className="w-12 h-12 rounded-xl bg-emerald-100 text-emerald-700 flex items-center justify-center">
                <TrendingUp className="w-6 h-6" />
              </div>
              <h4 className="text-base font-extrabold text-slate-900">4. AI Prediction</h4>
              <p className="text-xs text-slate-600 leading-relaxed font-medium">
                Analyze historical computer data to identify increasing performance or failure risk.
              </p>
            </div>

            {/* Card 5 */}
            <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm hover:shadow-md transition-all space-y-3">
              <div className="w-12 h-12 rounded-xl bg-red-100 text-red-700 flex items-center justify-center">
                <Power className="w-6 h-6" />
              </div>
              <h4 className="text-base font-extrabold text-slate-900">5. Remote Management</h4>
              <p className="text-xs text-slate-600 leading-relaxed font-medium">
                Allow authorized administrators to remotely lock, restart, or shut down computers.
              </p>
            </div>

            {/* Card 6 */}
            <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm hover:shadow-md transition-all space-y-3">
              <div className="w-12 h-12 rounded-xl bg-teal-100 text-teal-700 flex items-center justify-center">
                <CheckCircle2 className="w-6 h-6" />
              </div>
              <h4 className="text-base font-extrabold text-slate-900">6. Lab Readiness</h4>
              <p className="text-xs text-slate-600 leading-relaxed font-medium">
                Check whether computers meet the requirements for practical academic sessions.
              </p>
            </div>

            {/* Card 7 */}
            <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm hover:shadow-md transition-all space-y-3">
              <div className="w-12 h-12 rounded-xl bg-blue-100 text-blue-700 flex items-center justify-center">
                <PackageCheck className="w-6 h-6" />
              </div>
              <h4 className="text-base font-extrabold text-slate-900">7. Software Inventory</h4>
              <p className="text-xs text-slate-600 leading-relaxed font-medium">
                See which software is installed across all workstation computers.
              </p>
            </div>

            {/* Card 8 */}
            <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm hover:shadow-md transition-all space-y-3">
              <div className="w-12 h-12 rounded-xl bg-slate-200 text-slate-800 flex items-center justify-center">
                <Building2 className="w-6 h-6" />
              </div>
              <h4 className="text-base font-extrabold text-slate-900">8. Multi-Lab Management</h4>
              <p className="text-xs text-slate-600 leading-relaxed font-medium">
                Manage multiple college laboratories from a centralized platform.
              </p>
            </div>

          </div>
        </div>
      </section>

      {/* 5. How It Works (5-Step Process) */}
      <section id="how-it-works" className="py-16 sm:py-24 bg-white border-t border-slate-200">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 space-y-12">
          
          <div className="text-center space-y-3 max-w-3xl mx-auto">
            <h2 className="text-xs font-extrabold uppercase tracking-widest text-primary">Simple Workflow</h2>
            <h3 className="font-display text-3xl sm:text-4xl font-extrabold text-slate-900 tracking-tight">
              How NeuroSys Works
            </h3>
            <p className="text-sm text-slate-600 font-medium">
              The NeuroSys Agent runs in the background on supported Windows computers and securely communicates system information to the NeuroSys platform.
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-5 gap-4 relative">
            
            {/* Step 1 */}
            <div className="p-5 bg-surface-container-low rounded-xl border border-slate-200 text-center space-y-2">
              <div className="w-10 h-10 mx-auto rounded-full bg-primary text-white font-mono font-extrabold flex items-center justify-center text-sm">
                01
              </div>
              <h4 className="text-sm font-extrabold text-slate-900">Install Agent</h4>
              <p className="text-[11px] text-slate-600 font-medium leading-normal">
                Deploy 1-click agent installer on Windows PCs.
              </p>
            </div>

            {/* Step 2 */}
            <div className="p-5 bg-surface-container-low rounded-xl border border-slate-200 text-center space-y-2">
              <div className="w-10 h-10 mx-auto rounded-full bg-primary text-white font-mono font-extrabold flex items-center justify-center text-sm">
                02
              </div>
              <h4 className="text-sm font-extrabold text-slate-900">Connect PCs</h4>
              <p className="text-[11px] text-slate-600 font-medium leading-normal">
                Agent securely connects to Railway cloud platform.
              </p>
            </div>

            {/* Step 3 */}
            <div className="p-5 bg-surface-container-low rounded-xl border border-slate-200 text-center space-y-2">
              <div className="w-10 h-10 mx-auto rounded-full bg-primary text-white font-mono font-extrabold flex items-center justify-center text-sm">
                03
              </div>
              <h4 className="text-sm font-extrabold text-slate-900">Collect Data</h4>
              <p className="text-[11px] text-slate-600 font-medium leading-normal">
                1-second heartbeat & metric telemetry sampling.
              </p>
            </div>

            {/* Step 4 */}
            <div className="p-5 bg-surface-container-low rounded-xl border border-slate-200 text-center space-y-2">
              <div className="w-10 h-10 mx-auto rounded-full bg-primary text-white font-mono font-extrabold flex items-center justify-center text-sm">
                04
              </div>
              <h4 className="text-sm font-extrabold text-slate-900">Monitor & Analyze</h4>
              <p className="text-[11px] text-slate-600 font-medium leading-normal">
                AI engines process telemetry for risk analysis.
              </p>
            </div>

            {/* Step 5 */}
            <div className="p-5 bg-surface-container-low rounded-xl border border-slate-200 text-center space-y-2">
              <div className="w-10 h-10 mx-auto rounded-full bg-primary text-white font-mono font-extrabold flex items-center justify-center text-sm">
                05
              </div>
              <h4 className="text-sm font-extrabold text-slate-900">Admin Action</h4>
              <p className="text-[11px] text-slate-600 font-medium leading-normal">
                Remotely lock, restart, or resolve alerts.
              </p>
            </div>

          </div>
        </div>
      </section>

      {/* 6. Centralized Management Section */}
      <section id="centralized" className="py-16 sm:py-24 bg-surface-container-low border-t border-slate-200">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 space-y-12">
          
          <div className="text-center space-y-3 max-w-3xl mx-auto">
            <h2 className="text-xs font-extrabold uppercase tracking-widest text-primary">Architecture Overview</h2>
            <h3 className="font-display text-3xl sm:text-4xl font-extrabold text-slate-900 tracking-tight">
              Centralized Multi-Lab Architecture
            </h3>
            <p className="text-sm text-slate-600 font-medium">
              Manage multiple laboratories from one centralized platform while keeping lab access organized.
            </p>
          </div>

          {/* Conceptual Visual Tree Graphic */}
          <div className="max-w-4xl mx-auto bg-slate-900 text-white p-8 rounded-2xl shadow-xl border border-slate-800">
            <div className="text-center space-y-2 border-b border-slate-800 pb-6">
              <div className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-primary/20 text-indigo-300 border border-primary/30 text-xs font-extrabold">
                <Building2 className="w-4 h-4 text-primary" />
                <span>College Campus Infrastructure</span>
              </div>
              <h4 className="text-lg font-extrabold text-white">Centralized NeuroSys Platform</h4>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-6 pt-6 text-center">
              
              {/* Lab A */}
              <div className="p-4 bg-slate-800/80 rounded-xl border border-slate-700 space-y-3">
                <div className="flex items-center justify-center gap-1.5 text-xs font-bold text-emerald-400">
                  <Laptop className="w-4 h-4" />
                  <span>Lab A (Programming)</span>
                </div>
                <div className="space-y-1.5 text-[11px] font-mono text-slate-300">
                  <div className="p-1.5 bg-slate-900/60 rounded border border-slate-700/60">Workstation 01</div>
                  <div className="p-1.5 bg-slate-900/60 rounded border border-slate-700/60">Workstation 02</div>
                  <div className="p-1.5 bg-slate-900/60 rounded border border-slate-700/60">Workstation 03</div>
                </div>
              </div>

              {/* Lab B */}
              <div className="p-4 bg-slate-800/80 rounded-xl border border-slate-700 space-y-3">
                <div className="flex items-center justify-center gap-1.5 text-xs font-bold text-amber-400">
                  <Laptop className="w-4 h-4" />
                  <span>Lab B (AI & Data Science)</span>
                </div>
                <div className="space-y-1.5 text-[11px] font-mono text-slate-300">
                  <div className="p-1.5 bg-slate-900/60 rounded border border-slate-700/60">Workstation 01</div>
                  <div className="p-1.5 bg-slate-900/60 rounded border border-slate-700/60">Workstation 02</div>
                  <div className="p-1.5 bg-slate-900/60 rounded border border-slate-700/60">Workstation 03</div>
                </div>
              </div>

              {/* Lab C */}
              <div className="p-4 bg-slate-800/80 rounded-xl border border-slate-700 space-y-3">
                <div className="flex items-center justify-center gap-1.5 text-xs font-bold text-indigo-300">
                  <Laptop className="w-4 h-4" />
                  <span>Lab C (Networking)</span>
                </div>
                <div className="space-y-1.5 text-[11px] font-mono text-slate-300">
                  <div className="p-1.5 bg-slate-900/60 rounded border border-slate-700/60">Workstation 01</div>
                  <div className="p-1.5 bg-slate-900/60 rounded border border-slate-700/60">Workstation 02</div>
                </div>
              </div>

            </div>
          </div>
        </div>
      </section>

      {/* 7. AI Intelligence Section */}
      <section id="ai-intelligence" className="py-16 sm:py-24 bg-white border-t border-slate-200">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 space-y-12">
          
          <div className="text-center space-y-3 max-w-3xl mx-auto">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-purple-100 text-purple-700 text-xs font-extrabold">
              <Brain className="w-3.5 h-3.5" />
              <span>Predictive & Diagnostic Engine</span>
            </div>
            <h3 className="font-display text-3xl sm:text-4xl font-extrabold text-slate-900 tracking-tight">
              Turn system data into useful decisions.
            </h3>
            <p className="text-sm text-slate-600 font-medium">
              NeuroSys uses historical system information to identify trends and support administrators.
            </p>
          </div>

          {/* Conceptual Pipeline Diagram (No fake stats) */}
          <div className="grid grid-cols-1 md:grid-cols-5 gap-4 text-center">
            
            <div className="p-5 rounded-2xl bg-surface-container-low border border-slate-200 space-y-2">
              <div className="w-10 h-10 mx-auto rounded-xl bg-purple-100 text-purple-700 flex items-center justify-center font-bold">
                1
              </div>
              <h4 className="text-xs font-extrabold text-slate-900">Historical Data</h4>
              <p className="text-[11px] text-slate-600 font-medium">Aggregates long-term metric history</p>
            </div>

            <div className="p-5 rounded-2xl bg-surface-container-low border border-slate-200 space-y-2">
              <div className="w-10 h-10 mx-auto rounded-xl bg-purple-100 text-purple-700 flex items-center justify-center font-bold">
                2
              </div>
              <h4 className="text-xs font-extrabold text-slate-900">Trend Analysis</h4>
              <p className="text-[11px] text-slate-600 font-medium">Detects resource growth slope</p>
            </div>

            <div className="p-5 rounded-2xl bg-surface-container-low border border-slate-200 space-y-2">
              <div className="w-10 h-10 mx-auto rounded-xl bg-purple-100 text-purple-700 flex items-center justify-center font-bold">
                3
              </div>
              <h4 className="text-xs font-extrabold text-slate-900">Problem Detection</h4>
              <p className="text-[11px] text-slate-600 font-medium">Identifies persistent bottlenecks</p>
            </div>

            <div className="p-5 rounded-2xl bg-surface-container-low border border-slate-200 space-y-2">
              <div className="w-10 h-10 mx-auto rounded-xl bg-purple-100 text-purple-700 flex items-center justify-center font-bold">
                4
              </div>
              <h4 className="text-xs font-extrabold text-slate-900">Risk Analysis</h4>
              <p className="text-[11px] text-slate-600 font-medium">Evaluates system stability risk</p>
            </div>

            <div className="p-5 rounded-2xl bg-surface-container-low border border-slate-200 space-y-2">
              <div className="w-10 h-10 mx-auto rounded-xl bg-purple-100 text-purple-700 flex items-center justify-center font-bold">
                5
              </div>
              <h4 className="text-xs font-extrabold text-slate-900">Recommendation</h4>
              <p className="text-[11px] text-slate-600 font-medium">Actionable advice for admins</p>
            </div>

          </div>
        </div>
      </section>

      {/* 8. Remote Management Section */}
      <section className="py-16 sm:py-24 bg-surface-container-low border-t border-slate-200">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 space-y-12">
          
          <div className="text-center space-y-3 max-w-3xl mx-auto">
            <h2 className="text-xs font-extrabold uppercase tracking-widest text-primary">Remote Action Capabilities</h2>
            <h3 className="font-display text-3xl sm:text-4xl font-extrabold text-slate-900 tracking-tight">
              Manage computers remotely when action is required.
            </h3>
            <p className="text-sm text-slate-600 font-medium">
              Authorized administrators can perform predefined remote management actions without visiting every computer individually.
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-6 max-w-4xl mx-auto">
            
            {/* Lock */}
            <div className="bg-white p-6 rounded-2xl border border-slate-200 text-center space-y-3 shadow-sm">
              <div className="w-12 h-12 mx-auto rounded-xl bg-amber-100 text-amber-700 flex items-center justify-center">
                <Lock className="w-6 h-6" />
              </div>
              <h4 className="text-base font-extrabold text-slate-900">🔒 Lock Workstation</h4>
              <p className="text-xs text-slate-600 font-medium">Instantly lock active Windows sessions remotely.</p>
            </div>

            {/* Restart */}
            <div className="bg-white p-6 rounded-2xl border border-slate-200 text-center space-y-3 shadow-sm">
              <div className="w-12 h-12 mx-auto rounded-xl bg-indigo-100 text-primary flex items-center justify-center">
                <RotateCcw className="w-6 h-6" />
              </div>
              <h4 className="text-base font-extrabold text-slate-900">🔄 Restart System</h4>
              <p className="text-xs text-slate-600 font-medium">Safely trigger remote system restart for lab PCs.</p>
            </div>

            {/* Shutdown */}
            <div className="bg-white p-6 rounded-2xl border border-slate-200 text-center space-y-3 shadow-sm">
              <div className="w-12 h-12 mx-auto rounded-xl bg-red-100 text-red-700 flex items-center justify-center">
                <Power className="w-6 h-6" />
              </div>
              <h4 className="text-base font-extrabold text-slate-900">🔴 Remote Shutdown</h4>
              <p className="text-xs text-slate-600 font-medium">Execute authorized remote power shutdown commands.</p>
            </div>

          </div>

          <div className="text-center text-xs font-semibold text-slate-500 max-w-lg mx-auto bg-white p-3 rounded-lg border border-slate-200">
            🔒 <em>Note: Remote management actions are accessible exclusively to authenticated administrators after login.</em>
          </div>
        </div>
      </section>

      {/* 9. Lab Readiness Conceptual Section */}
      <section className="py-16 sm:py-24 bg-white border-t border-slate-200">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 space-y-12">
          
          <div className="text-center space-y-3 max-w-3xl mx-auto">
            <h2 className="text-xs font-extrabold uppercase tracking-widest text-primary">Academic Pre-Flight Check</h2>
            <h3 className="font-display text-3xl sm:text-4xl font-extrabold text-slate-900 tracking-tight">
              Academic Lab Readiness Verification
            </h3>
            <p className="text-sm text-slate-600 font-medium">
              Verify whether workstations meet software and hardware criteria prior to practical exam sessions.
            </p>
          </div>

          {/* Conceptual Workflow Example */}
          <div className="max-w-3xl mx-auto bg-surface-container-low p-6 rounded-2xl border border-slate-200 space-y-6">
            <div className="flex items-center justify-between border-b border-slate-200 pb-3">
              <div className="flex items-center gap-2">
                <CheckCircle2 className="w-5 h-5 text-emerald-600" />
                <span className="text-sm font-extrabold text-slate-900">Practical Session Example: Java Programming Practical</span>
              </div>
              <span className="text-[11px] font-mono font-bold bg-emerald-100 text-emerald-800 px-2.5 py-0.5 rounded-full">
                Illustrative Profile
              </span>
            </div>

            <div className="grid grid-cols-2 sm:grid-cols-5 gap-3 text-center">
              <div className="p-3 bg-white rounded-lg border border-slate-200 text-xs font-bold text-slate-800 flex items-center justify-center gap-1">
                <Check className="w-4 h-4 text-emerald-600" /> Java JDK
              </div>
              <div className="p-3 bg-white rounded-lg border border-slate-200 text-xs font-bold text-slate-800 flex items-center justify-center gap-1">
                <Check className="w-4 h-4 text-emerald-600" /> VS Code
              </div>
              <div className="p-3 bg-white rounded-lg border border-slate-200 text-xs font-bold text-slate-800 flex items-center justify-center gap-1">
                <Check className="w-4 h-4 text-emerald-600" /> Git
              </div>
              <div className="p-3 bg-white rounded-lg border border-slate-200 text-xs font-bold text-slate-800 flex items-center justify-center gap-1">
                <Check className="w-4 h-4 text-emerald-600" /> Internet
              </div>
              <div className="p-3 bg-white rounded-lg border border-slate-200 text-xs font-bold text-slate-800 flex items-center justify-center gap-1">
                <Check className="w-4 h-4 text-emerald-600" /> Free Disk
              </div>
            </div>

            <div className="p-4 bg-white rounded-xl border border-slate-200 flex flex-wrap items-center justify-between gap-4 text-xs font-bold">
              <span className="text-slate-900">Lab Readiness Summary (40 Workstations)</span>
              <div className="flex items-center gap-4">
                <span className="text-emerald-700">✓ Ready: 36</span>
                <span className="text-amber-700">⚠️ Needs Attention: 3</span>
                <span className="text-red-700">❌ Not Ready: 1</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* 10. Security Section */}
      <section id="security" className="py-16 sm:py-24 bg-surface-container-low border-t border-slate-200">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 space-y-12">
          
          <div className="text-center space-y-3 max-w-3xl mx-auto">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-slate-200 text-slate-800 text-xs font-extrabold">
              <ShieldCheck className="w-3.5 h-3.5" />
              <span>Role-Based Access Control</span>
            </div>
            <h3 className="font-display text-3xl sm:text-4xl font-extrabold text-slate-900 tracking-tight">
              Built with controlled access in mind.
            </h3>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 max-w-5xl mx-auto">
            <div className="bg-white p-6 rounded-2xl border border-slate-200 space-y-2">
              <h4 className="text-sm font-extrabold text-slate-900">JWT Authentication Required</h4>
              <p className="text-xs text-slate-600 font-medium leading-relaxed">
                All management dashboard features require authenticated administrator sessions.
              </p>
            </div>

            <div className="bg-white p-6 rounded-2xl border border-slate-200 space-y-2">
              <h4 className="text-sm font-extrabold text-slate-900">Authorized Actions Only</h4>
              <p className="text-xs text-slate-600 font-medium leading-relaxed">
                Administrative operations such as Remote Power commands require explicit authorization.
              </p>
            </div>

            <div className="bg-white p-6 rounded-2xl border border-slate-200 space-y-2">
              <h4 className="text-sm font-extrabold text-slate-900">Protected Public Boundary</h4>
              <p className="text-xs text-slate-600 font-medium leading-relaxed">
                Public visitors cannot access private system metrics, computer names, IP addresses, or logs.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* 11. About & Target Users Section */}
      <section id="about" className="py-16 sm:py-24 bg-white border-t border-slate-200">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 space-y-12">
          
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-12 items-center">
            
            <div className="space-y-4">
              <h2 className="text-xs font-extrabold uppercase tracking-widest text-primary">About NeuroSys</h2>
              <h3 className="font-display text-3xl font-extrabold text-slate-900 tracking-tight">
                Designed to simplify computer laboratory management for educational institutions.
              </h3>
              <p className="text-sm text-slate-700 font-medium leading-relaxed">
                Instead of checking individual computers manually, administrators can use one centralized platform to monitor system health, identify persistent problems, maintain software readiness, and take action when required.
              </p>
            </div>

            <div className="bg-surface-container-low p-6 rounded-2xl border border-slate-200 space-y-4">
              <h4 className="text-sm font-extrabold text-slate-900 flex items-center gap-2">
                <Users className="w-5 h-5 text-primary" />
                Designed for College Computer Labs
              </h4>
              <div className="grid grid-cols-2 gap-3 text-xs font-bold text-slate-800">
                <div className="p-3 bg-white rounded-lg border border-slate-200">✓ Lab Administrators</div>
                <div className="p-3 bg-white rounded-lg border border-slate-200">✓ IT Administrators</div>
                <div className="p-3 bg-white rounded-lg border border-slate-200">✓ System Administrators</div>
                <div className="p-3 bg-white rounded-lg border border-slate-200">✓ Authorized Lab Staff</div>
              </div>
            </div>

          </div>
        </div>
      </section>

      {/* 12. Call To Action (CTA) */}
      <section className="py-16 bg-slate-900 text-white text-center">
        <div className="max-w-3xl mx-auto px-4 space-y-6">
          <h2 className="font-display text-3xl sm:text-4xl font-extrabold tracking-tight">
            Ready to manage your computer lab smarter?
          </h2>
          <p className="text-sm text-slate-400 font-medium">
            Sign in to access the NeuroSys management dashboard.
          </p>
          <div>
            <button
              onClick={() => navigate('/login')}
              className="px-8 py-3.5 rounded-xl bg-primary hover:bg-primary-container text-white text-sm font-extrabold transition-all cursor-pointer shadow-lg shadow-primary/30 inline-flex items-center gap-2"
            >
              <span>Sign In to NeuroSys</span>
              <ArrowRight className="w-4 h-4" />
            </button>
          </div>
        </div>
      </section>

      {/* 13. Footer */}
      <footer className="bg-slate-950 text-slate-400 py-12 border-t border-slate-800 text-xs">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 space-y-8">
          <div className="flex flex-col md:flex-row items-center justify-between gap-4">
            
            <div className="flex items-center gap-2.5">
              <div className="w-8 h-8 rounded-lg bg-primary text-white flex items-center justify-center font-bold">
                <BrainCircuit className="w-5 h-5" />
              </div>
              <span className="font-display text-lg font-bold text-white tracking-tight">
                NeuroSys
              </span>
            </div>

            <div className="flex flex-wrap items-center gap-6 font-bold text-slate-300">
              <button onClick={() => scrollToSection('hero')} className="hover:text-white transition-colors">Home</button>
              <button onClick={() => scrollToSection('features')} className="hover:text-white transition-colors">Features</button>
              <button onClick={() => scrollToSection('how-it-works')} className="hover:text-white transition-colors">How It Works</button>
              <button onClick={() => scrollToSection('security')} className="hover:text-white transition-colors">Security</button>
              <button onClick={() => scrollToSection('about')} className="hover:text-white transition-colors">About</button>
              <button onClick={() => navigate('/login')} className="text-primary hover:underline font-bold">Sign In</button>
            </div>
          </div>

          <div className="pt-6 border-t border-slate-800/80 flex flex-col sm:flex-row items-center justify-between gap-4 text-slate-500 text-[11px]">
            <p>© {new Date().getFullYear()} NeuroSys Platform. AI-Powered Computer Lab Management for Educational Institutions.</p>
            <p>Protected Management Boundary</p>
          </div>
        </div>
      </footer>

    </div>
  );
};

export default LandingPage;
