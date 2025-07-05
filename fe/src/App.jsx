import React, { useState, useEffect } from 'react';
import { Play, Trash2, Clock, User, Monitor, RefreshCw } from 'lucide-react';
import './test.css';

const API_BASE = 'http://localhost:8080/api/labs';

const LabPlatform = () => {
  const [labs, setLabs] = useState([]);
  const [labTypes, setLabTypes] = useState([]);
  const [selectedType, setSelectedType] = useState('');
  const [userId, setUserId] = useState('user123');
  const [isCreating, setIsCreating] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    fetchLabTypes();
    if (userId) {
      fetchUserLabs();
    }
  }, [userId]);

  const fetchLabTypes = async () => {
    try {
      const response = await fetch(`${API_BASE}/types`);
      if (!response.ok) throw new Error('Failed to fetch lab types');
      const types = await response.json();
      setLabTypes(types);
      setSelectedType(types[0] || '');
    } catch (error) {
      console.error('Error fetching lab types:', error);
      setError('Failed to load lab types');
    }
  };

  const fetchUserLabs = async () => {
    try {
      const response = await fetch(`${API_BASE}/user/${userId}`);
      if (!response.ok) throw new Error('Failed to fetch user labs');
      const userLabs = await response.json();
      setLabs(userLabs);
      setError('');
    } catch (error) {
      console.error('Error fetching labs:', error);
      setError('Failed to load labs');
    }
  };

  const createLab = async () => {
    if (!selectedType || !userId.trim()) return;
    
    setIsCreating(true);
    setError('');
    try {
      const response = await fetch(`${API_BASE}/create`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          userId: userId.trim(),
          labType: selectedType,
          duration: 7200
        }),
      });

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`Failed to create lab: ${errorText}`);
      }

      const newLab = await response.json();
      setLabs(prev => [...prev, newLab]);
    } catch (error) {
      console.error('Error creating lab:', error);
      setError(error.message);
    } finally {
      setIsCreating(false);
    }
  };

  const deleteLab = async (labId) => {
    try {
      const response = await fetch(`${API_BASE}/${labId}`, {
        method: 'DELETE',
      });

      if (!response.ok) throw new Error('Failed to delete lab');
      
      setLabs(prev => prev.filter(lab => lab.id !== labId));
    } catch (error) {
      console.error('Error deleting lab:', error);
      setError('Failed to delete lab');
    }
  };

  const refreshLabStatus = async (labId) => {
    try {
      const response = await fetch(`${API_BASE}/${labId}/status`);
      if (!response.ok) throw new Error('Failed to get lab status');
      
      const status = await response.text();
      setLabs(prev => prev.map(lab => 
        lab.id === labId ? { ...lab, status } : lab
      ));
    } catch (error) {
      console.error('Error refreshing lab status:', error);
    }
  };

  const getLabTypeIcon = (type) => {
    const icons = {
      docker: '🐳',
      python: '🐍',
      nodejs: '📦',
      kubernetes: '☸️'
    };
    return icons[type] || '💻';
  };

  const formatTimeRemaining = (expiresAt) => {
    const now = new Date();
    const expires = new Date(expiresAt);
    const diff = expires - now;
    
    if (diff <= 0) return 'Expired';
    
    const hours = Math.floor(diff / (1000 * 60 * 60));
    const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
    
    return `${hours}h ${minutes}m`;
  };

  const getStatusColor = (status) => {
    switch (status) {
      case 'Running':
        return '#10b981'; // green
      case 'Creating':
        return '#f59e0b'; // amber
      case 'Stopped':
        return '#ef4444'; // red
      default:
        return '#6b7280'; // gray
    }
  };

  return (
    <div className="lab-platform">
      <div className="lab-container">
        {/* Header */}
        <div className="lab-header">
          <h1 className="lab-title">Lab Platform Dashboard</h1>
          <p className="lab-subtitle">Create and manage your development lab environments</p>
        </div>

        {/* Error Display */}
        {error && (
          <div className="error-message">
            {error}
          </div>
        )}

        {/* User Info */}
        <div className="user-card">
          <div className="user-info">
            <User size={20} color="#6b7280" />
            <span style={{ fontWeight: '500' }}>User ID:</span>
            <input
              type="text"
              value={userId}
              onChange={(e) => setUserId(e.target.value)}
              className="lab-input"
              placeholder="Enter user ID"
            />
          </div>
        </div>

        {/* Create Lab Section */}
        <div className="lab-card">
          <h2 style={{ fontSize: '20px', fontWeight: '600', marginBottom: '16px', margin: '0 0 16px 0' }}>
            Create New Lab
          </h2>
          <div className="create-section">
            <div className="create-form-group">
              <label className="lab-label">Lab Type</label>
              <select
                value={selectedType}
                onChange={(e) => setSelectedType(e.target.value)}
                className="lab-select"
              >
                {labTypes.map(type => (
                  <option key={type} value={type}>
                    {getLabTypeIcon(type)} {type.charAt(0).toUpperCase() + type.slice(1)}
                  </option>
                ))}
              </select>
            </div>
            <button
              onClick={createLab}
              disabled={isCreating || !selectedType || !userId.trim()}
              className="lab-button"
            >
              <Play size={16} />
              {isCreating ? 'Creating...' : 'Create Lab'}
            </button>
          </div>
        </div>

        {/* Labs Grid */}
        <div className="labs-grid">
          {labs.map((lab) => (
            <div key={lab.id} className="lab-item-card">
              {/* Lab Header */}
              <div className="lab-item-header">
                <div className="lab-item-info">
                  <span className="lab-item-icon">{getLabTypeIcon(lab.labType)}</span>
                  <div>
                    <h3 className="lab-item-title">
                      {lab.labType.charAt(0).toUpperCase() + lab.labType.slice(1)} Lab
                    </h3>
                    <p className="lab-item-id">{lab.id}</p>
                  </div>
                </div>
                <span className="lab-badge">{lab.labType}</span>
              </div>

              {/* Lab Details */}
              <div className="lab-details">
                <div className="lab-detail-item">
                  <Clock size={16} color="#6b7280" />
                  <span className="lab-detail-label">Expires in:</span>
                  <span className="lab-detail-value">{formatTimeRemaining(lab.expiresAt)}</span>
                </div>
                <div className="lab-detail-item">
                  <Monitor size={16} color="#6b7280" />
                  <span className="lab-detail-label">Status:</span>
                  <span className={`lab-detail-value status-${lab.status.toLowerCase()}`}>
                    {lab.status}
                  </span>
                </div>
              </div>

              {/* Actions */}
              <div className="lab-actions">
                <a
                  href={lab.accessUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="access-button"
                >
                  Access Lab
                </a>
                <button
                  onClick={() => refreshLabStatus(lab.id)}
                  className="icon-button refresh-button"
                  title="Refresh Status"
                >
                  <RefreshCw size={16} />
                </button>
                <button
                  onClick={() => deleteLab(lab.id)}
                  className="icon-button delete-button"
                  title="Delete Lab"
                >
                  <Trash2 size={16} />
                </button>
              </div>
            </div>
          ))}
        </div>

        {labs.length === 0 && (
          <div className="empty-state">
            <Monitor size={48} color="#9ca3af" className="empty-state-icon" />
            <p className="empty-state-title">No labs created yet</p>
            <p className="empty-state-subtitle">Create your first lab to get started!</p>
          </div>
        )}

        {/* Cluster Info */}
        <div className="cluster-info">
          <h3 className="cluster-title">Cluster Information</h3>
          <div className="cluster-grid">
            <div className="cluster-item">
              <span className="cluster-label">Master Node:</span>
              <span className="cluster-value">192.168.122.211 (2 CPU, 3GB RAM)</span>
            </div>
            <div className="cluster-item">
              <span className="cluster-label">Worker Node:</span>
              <span className="cluster-value">192.168.122.93 (2 CPU, 4GB RAM)</span>
            </div>
            <div className="cluster-item">
              <span className="cluster-label">Runtime:</span>
              <span className="cluster-value">containerd://2.0.5</span>
            </div>
            <div className="cluster-item">
              <span className="cluster-label">Kubernetes:</span>
              <span className="cluster-value">v1.33.1</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default LabPlatform;