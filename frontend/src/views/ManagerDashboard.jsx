import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import API from '../services/api';

const MONTHS = ['January','February','March','April','May','June','July','August','September','October','November','December'];
const DAYS = ['Sun','Mon','Tue','Wed','Thu','Fri','Sat'];

export default function ManagerDashboard() {
  const [requests, setRequests] = useState([]);
  const [calendarData, setCalendarData] = useState([]);
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState('requests');
  const [commentModal, setCommentModal] = useState({ open: false, requestId: null, action: null });
  const [comment, setComment] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [calendarDate, setCalendarDate] = useState(new Date());
  const navigate = useNavigate();

  useEffect(() => {
    const stored = localStorage.getItem('user');
    if (stored) setUser(JSON.parse(stored));
    fetchData();
  }, []);

  const fetchData = async () => {
    setLoading(true);
    try {
      const [reqRes, calRes] = await Promise.all([API.get('/manager/requests'), API.get('/manager/team-calendar')]);
      setRequests(reqRes.data);
      setCalendarData(calRes.data.filter(r => r.status === 'APPROVED' || r.status === 'PENDING'));
    } catch (err) {
      console.error('Manager data error:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleAction = async () => {
    if (!commentModal.requestId) return;
    setSubmitting(true);
    try {
      const endpoint = commentModal.action === 'approve'
        ? `/manager/requests/${commentModal.requestId}/approve`
        : `/manager/requests/${commentModal.requestId}/reject`;
      await API.put(endpoint, { comment });
      setCommentModal({ open: false, requestId: null, action: null });
      setComment('');
      fetchData();
    } catch (err) {
      alert(err.response?.data || 'Action failed');
    } finally {
      setSubmitting(false);
    }
  };

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    navigate('/login');
  };

  const statusBadge = (s) => {
    const map = { APPROVED: 'badge-approved', PENDING: 'badge-pending', REJECTED: 'badge-rejected' };
    return <span className={`badge ${map[s] || 'badge-cancelled'}`}>{s}</span>;
  };

  // Calendar logic
  const year = calendarDate.getFullYear();
  const month = calendarDate.getMonth();
  const firstDay = new Date(year, month, 1).getDay();
  const daysInMonth = new Date(year, month + 1, 0).getDate();
  const prevMonth = () => setCalendarDate(new Date(year, month - 1, 1));
  const nextMonth = () => setCalendarDate(new Date(year, month + 1, 1));

  const getEmployeesOnLeave = (day) => {
    const date = new Date(year, month, day);
    return calendarData.filter(r => date >= new Date(r.startDate) && date <= new Date(r.endDate));
  };

  const pending = requests.filter(r => r.status === 'PENDING');

  return (
    <div className="page">
      <header className="dash-header">
        <div>
          <strong>LeavePortal</strong>
          <span> — Manager Dashboard | {user?.username}</span>
        </div>
        <div className="header-actions">
          <button className="btn btn-secondary btn-small" onClick={fetchData}>Refresh</button>
          <button className="btn btn-danger btn-small" onClick={handleLogout}>Logout</button>
        </div>
      </header>

      {/* Stats */}
      <div className="stats-row">
        {[
          { label: 'Total', value: requests.length },
          { label: 'Pending', value: pending.length },
          { label: 'Approved', value: requests.filter(r => r.status === 'APPROVED').length },
          { label: 'Rejected', value: requests.filter(r => r.status === 'REJECTED').length },
        ].map(s => (
          <div key={s.label} className="stat-card">
            <div className="stat-value">{s.value}</div>
            <div className="stat-label">{s.label}</div>
          </div>
        ))}
      </div>

      <div className="dash-main">
        {/* Tabs */}
        <div className="tabs">
          <button className={`tab-btn ${activeTab === 'requests' ? 'active' : ''}`} onClick={() => setActiveTab('requests')}>
            Leave Requests {pending.length > 0 && <span className="badge-count">{pending.length}</span>}
          </button>
          <button className={`tab-btn ${activeTab === 'calendar' ? 'active' : ''}`} onClick={() => setActiveTab('calendar')}>
            Team Calendar
          </button>
        </div>

        {/* Requests Tab */}
        {activeTab === 'requests' && (
          <div className="panel">
            <h2>Team Leave Requests</h2>
            {requests.length === 0 ? (
              <p className="empty-state">No leave requests from your team yet.</p>
            ) : (
              <div className="table-container">
                <table>
                  <thead>
                    <tr>
                      <th>Employee</th><th>Leave Type</th><th>Start</th><th>End</th>
                      <th>Days</th><th>Reason</th><th>Status</th><th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {requests.map(req => (
                      <tr key={req.id}>
                        <td>
                          <strong>{req.user.firstName} {req.user.lastName}</strong>
                          <br /><small className="muted">@{req.user.username}</small>
                        </td>
                        <td>{req.leaveType.name}</td>
                        <td>{req.startDate}</td>
                        <td>{req.endDate}</td>
                        <td><strong>{req.numberOfDays}</strong></td>
                        <td className="truncate">{req.reason}</td>
                        <td>{statusBadge(req.status)}</td>
                        <td>
                          {req.status === 'PENDING' ? (
                            <div className="action-btns">
                              <button className="btn btn-success btn-small" onClick={() => setCommentModal({ open: true, requestId: req.id, action: 'approve' })}>Approve</button>
                              <button className="btn btn-danger btn-small" onClick={() => setCommentModal({ open: true, requestId: req.id, action: 'reject' })}>Reject</button>
                            </div>
                          ) : (
                            <span className="muted">{req.managerComment || 'No comment'}</span>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        )}

        {/* Calendar Tab */}
        {activeTab === 'calendar' && (
          <div className="panel">
            <div className="calendar-header">
              <h2>Team Leave Calendar — {MONTHS[month]} {year}</h2>
              <div>
                <button className="btn btn-secondary btn-small" onClick={prevMonth}>‹</button>
                <button className="btn btn-secondary btn-small" onClick={nextMonth}>›</button>
              </div>
            </div>
            <div className="cal-grid">
              {DAYS.map(d => <div key={d} className="cal-day-name">{d}</div>)}
              {Array.from({ length: firstDay }).map((_, i) => <div key={`e${i}`} />)}
              {Array.from({ length: daysInMonth }).map((_, i) => {
                const day = i + 1;
                const today = new Date();
                const isToday = today.getDate() === day && today.getMonth() === month && today.getFullYear() === year;
                const onLeave = getEmployeesOnLeave(day);
                return (
                  <div key={day} className={`cal-cell ${isToday ? 'cal-today' : ''}`}>
                    <span className="cal-date">{day}</span>
                    {onLeave.slice(0, 2).map(r => (
                      <div key={r.id} className={`cal-event ${r.status === 'APPROVED' ? 'cal-approved' : 'cal-pending'}`}>
                        {r.user.firstName} {r.user.lastName?.[0]}.
                      </div>
                    ))}
                    {onLeave.length > 2 && <div className="cal-more">+{onLeave.length - 2} more</div>}
                  </div>
                );
              })}
            </div>
            <div className="cal-legend">
              <span><span className="legend-dot approved" /> Approved</span>
              <span><span className="legend-dot pending" /> Pending</span>
            </div>
          </div>
        )}
      </div>

      {/* Approve / Reject Modal */}
      {commentModal.open && (
        <div className="modal-overlay">
          <div className="modal-content">
            <div className="modal-header">
              <h3>{commentModal.action === 'approve' ? '✓ Approve Leave Request' : '✗ Reject Leave Request'}</h3>
              <button className="close-btn" onClick={() => { setCommentModal({ open: false, requestId: null, action: null }); setComment(''); }}>✕</button>
            </div>
            <div className="modal-body">
              <label>Comment / Feedback (optional)</label>
              <textarea rows="3" value={comment} onChange={e => setComment(e.target.value)} placeholder="Add a comment for the employee..." />
              <div className="modal-footer">
                <button className="btn btn-secondary btn-small" onClick={() => { setCommentModal({ open: false, requestId: null, action: null }); setComment(''); }}>Cancel</button>
                <button className={`btn btn-small ${commentModal.action === 'approve' ? 'btn-success' : 'btn-danger'}`} onClick={handleAction} disabled={submitting}>
                  {submitting ? 'Processing...' : (commentModal.action === 'approve' ? 'Confirm Approve' : 'Confirm Reject')}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
