import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import API from '../services/api';

export default function EmployeeDashboard() {
  const [balances, setBalances] = useState([]);
  const [requests, setRequests] = useState([]);
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  // Apply modal state
  const [showModal, setShowModal] = useState(false);
  const [leaveTypeId, setLeaveTypeId] = useState('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [reason, setReason] = useState('');
  const [applyError, setApplyError] = useState('');
  const [applySuccess, setApplySuccess] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const navigate = useNavigate();

  useEffect(() => {
    const stored = localStorage.getItem('user');
    if (stored) setUser(JSON.parse(stored));
    fetchData();
  }, []);

  const fetchData = async () => {
    setLoading(true);
    try {
      const [balRes, reqRes] = await Promise.all([API.get('/leaves/balances'), API.get('/leaves/my-requests')]);
      setBalances(balRes.data);
      setRequests(reqRes.data);
    } catch (err) {
      console.error('Error fetching employee data', err);
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    navigate('/login');
  };

  const handleCancel = async (id) => {
    if (!window.confirm('Cancel this pending leave request?')) return;
    try {
      await API.put(`/leaves/cancel/${id}`);
      fetchData();
    } catch (err) {
      alert(err.response?.data || 'Failed to cancel request');
    }
  };

  const handleApply = async (e) => {
    e.preventDefault();
    setApplyError('');
    setApplySuccess(false);
    setSubmitting(true);
    try {
      await API.post('/leaves/apply', { leaveTypeId: parseInt(leaveTypeId), startDate, endDate, reason });
      setApplySuccess(true);
      setLeaveTypeId(''); setStartDate(''); setEndDate(''); setReason('');
      fetchData();
      setTimeout(() => { setShowModal(false); setApplySuccess(false); }, 1500);
    } catch (err) {
      setApplyError(err.response?.data || 'Failed to apply for leave.');
    } finally {
      setSubmitting(false);
    }
  };

  const requestedDays = () => {
    if (!startDate || !endDate) return 0;
    const diff = new Date(endDate) - new Date(startDate);
    return diff < 0 ? 0 : Math.ceil(diff / 86400000) + 1;
  };

  const statusBadge = (s) => {
    const map = { APPROVED: 'badge-approved', PENDING: 'badge-pending', REJECTED: 'badge-rejected' };
    return <span className={`badge ${map[s] || 'badge-cancelled'}`}>{s}</span>;
  };

  if (loading && balances.length === 0) return <div className="loading">Loading...</div>;

  return (
    <div className="page">
      <header className="dash-header">
        <div>
          <strong>LeavePortal</strong>
          <span> — Employee Dashboard | {user?.username}</span>
        </div>
        <div className="header-actions">
          <button className="btn btn-secondary btn-small" onClick={fetchData}>Refresh</button>
          <button className="btn btn-danger btn-small" onClick={handleLogout}>Logout</button>
        </div>
      </header>

      <main className="dash-main">
        {/* Leave Balances */}
        <section>
          <div className="section-header">
            <h2>Leave Balances</h2>
            <button className="btn btn-primary btn-small" onClick={() => setShowModal(true)}>+ Apply Leave</button>
          </div>
          <div className="balance-grid">
            {balances.map(bal => (
              <div key={bal.id} className="balance-card">
                <div>
                  <strong>{bal.leaveType.name}</strong>
                  <p>{bal.leaveType.description}</p>
                  <small>Allocated: {bal.allocatedDays} &nbsp; Used: {bal.usedDays}</small>
                </div>
                <div className="balance-remaining">
                  <span>{bal.remainingDays}</span>
                  <small>remaining</small>
                </div>
              </div>
            ))}
          </div>
        </section>

        {/* Leave History Table */}
        <section className="panel">
          <h2>Leave History</h2>
          {requests.length === 0 ? (
            <p className="empty-state">No leave requests yet. Apply using the button above.</p>
          ) : (
            <div className="table-container">
              <table>
                <thead>
                  <tr>
                    <th>Leave Type</th><th>Start</th><th>End</th><th>Days</th>
                    <th>Reason</th><th>Status</th><th>Manager Feedback</th><th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {requests.map(req => (
                    <tr key={req.id}>
                      <td>{req.leaveType.name}</td>
                      <td>{req.startDate}</td>
                      <td>{req.endDate}</td>
                      <td>{req.numberOfDays}</td>
                      <td className="truncate">{req.reason}</td>
                      <td>{statusBadge(req.status)}</td>
                      <td>{req.managerComment || '-'}</td>
                      <td>
                        {req.status === 'PENDING'
                          ? <button className="btn btn-danger btn-small" onClick={() => handleCancel(req.id)}>Cancel</button>
                          : <span className="muted">Locked</span>}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
      </main>

      {/* Apply Leave Modal */}
      {showModal && (
        <div className="modal-overlay">
          <div className="modal-content">
            <div className="modal-header">
              <h3>Apply for Leave</h3>
              <button className="close-btn" onClick={() => { setShowModal(false); setApplyError(''); }}>✕</button>
            </div>
            <form onSubmit={handleApply} className="modal-body">
              {applyError && <p className="msg msg-error">{applyError}</p>}
              {applySuccess && <p className="msg msg-success">Leave applied successfully!</p>}
              <label>Leave Type</label>
              <select value={leaveTypeId} onChange={e => setLeaveTypeId(e.target.value)} required>
                <option value="">Select a leave type</option>
                {balances.map(b => (
                  <option key={b.leaveType.id} value={b.leaveType.id}>
                    {b.leaveType.name} (Remaining: {b.remainingDays} days)
                  </option>
                ))}
              </select>
              <div className="row-2">
                <div>
                  <label>Start Date</label>
                  <input type="date" value={startDate} onChange={e => setStartDate(e.target.value)} required min={new Date().toISOString().split('T')[0]} />
                </div>
                <div>
                  <label>End Date</label>
                  <input type="date" value={endDate} onChange={e => setEndDate(e.target.value)} required min={startDate || new Date().toISOString().split('T')[0]} />
                </div>
              </div>
              {requestedDays() > 0 && <p className="days-preview">Total Requested: {requestedDays()} day(s)</p>}
              <label>Reason</label>
              <textarea rows="3" value={reason} onChange={e => setReason(e.target.value)} placeholder="Reason for leave..." required />
              <div className="modal-footer">
                <button type="button" className="btn btn-secondary btn-small" onClick={() => { setShowModal(false); setApplyError(''); }}>Cancel</button>
                <button type="submit" className="btn btn-primary btn-small" disabled={submitting || applySuccess}>
                  {submitting ? 'Submitting...' : 'Submit Request'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
