import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import API from '../services/api';
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
  PieChart, Pie, Cell, Legend
} from 'recharts';

const COLORS = ['#6366f1', '#10b981', '#f59e0b', '#f43f5e', '#3b82f6', '#a78bfa', '#34d399'];

export default function AdminDashboard() {
  const [activeTab, setActiveTab] = useState('reports');
  const [user, setUser] = useState(null);
  const navigate = useNavigate();

  const [employees, setEmployees] = useState([]);
  const [departments, setDepartments] = useState([]);
  const [leaveTypes, setLeaveTypes] = useState([]);
  const [leaveUsage, setLeaveUsage] = useState([]);
  const [deptStats, setDeptStats] = useState([]);
  const [defaulters, setDefaulters] = useState([]);

  const [empModal, setEmpModal] = useState({ open: false, mode: 'add', data: null });
  const [deptModal, setDeptModal] = useState({ open: false, mode: 'add', data: null });
  const [ltModal, setLtModal] = useState({ open: false, mode: 'add', data: null });
  const [formError, setFormError] = useState('');
  const [formSuccess, setFormSuccess] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const emptyEmp = { username: '', password: '', firstName: '', lastName: '', email: '', role: 'EMPLOYEE', departmentId: '', managerId: '' };
  const [empForm, setEmpForm] = useState(emptyEmp);
  const [deptForm, setDeptForm] = useState({ name: '', description: '' });
  const [ltForm, setLtForm] = useState({ name: '', defaultDays: '', description: '' });

  useEffect(() => {
    const stored = localStorage.getItem('user');
    if (stored) setUser(JSON.parse(stored));
    fetchAll();
  }, []);

  const fetchAll = async () => {
    try {
      const [empRes, deptRes, ltRes, usageRes, deptStRes, defRes] = await Promise.all([
        API.get('/admin/employees'), API.get('/admin/departments'), API.get('/admin/leave-types'),
        API.get('/admin/reports/leave-usage'), API.get('/admin/reports/department-stats'), API.get('/admin/reports/defaulters')
      ]);
      setEmployees(empRes.data); setDepartments(deptRes.data); setLeaveTypes(ltRes.data);
      setLeaveUsage(usageRes.data); setDeptStats(deptStRes.data); setDefaulters(defRes.data);
    } catch (err) { console.error('Admin fetch error:', err); }
  };

  const handleLogout = () => { localStorage.removeItem('token'); localStorage.removeItem('user'); navigate('/login'); };

  // Employee CRUD
  const openAddEmp = () => { setEmpForm(emptyEmp); setFormError(''); setFormSuccess(''); setEmpModal({ open: true, mode: 'add', data: null }); };
  const openEditEmp = (emp) => {
    setEmpForm({ username: emp.username, password: '', firstName: emp.firstName, lastName: emp.lastName, email: emp.email, role: emp.role, departmentId: emp.department?.id || '', managerId: emp.manager?.id || '', active: emp.active });
    setFormError(''); setFormSuccess(''); setEmpModal({ open: true, mode: 'edit', data: emp });
  };
  const submitEmp = async (e) => {
    e.preventDefault(); setFormError(''); setSubmitting(true);
    try {
      if (empModal.mode === 'add') await API.post('/admin/employees', { ...empForm, departmentId: empForm.departmentId || null, managerId: empForm.managerId || null });
      else await API.put(`/admin/employees/${empModal.data.id}`, { ...empForm, departmentId: empForm.departmentId || null, managerId: empForm.managerId || null, active: empForm.active !== false });
      setFormSuccess(empModal.mode === 'add' ? 'Employee created!' : 'Employee updated!');
      fetchAll();
      setTimeout(() => setEmpModal({ open: false, mode: 'add', data: null }), 1000);
    } catch (err) { setFormError(err.response?.data || 'Operation failed'); }
    finally { setSubmitting(false); }
  };
  const deleteEmp = async (id) => {
    if (!window.confirm('Deactivate this employee?')) return;
    try { await API.delete(`/admin/employees/${id}`); fetchAll(); }
    catch (err) { alert(err.response?.data || 'Delete failed'); }
  };

  // Department CRUD
  const openAddDept = () => { setDeptForm({ name: '', description: '' }); setFormError(''); setFormSuccess(''); setDeptModal({ open: true, mode: 'add', data: null }); };
  const openEditDept = (dept) => { setDeptForm({ name: dept.name, description: dept.description || '' }); setFormError(''); setFormSuccess(''); setDeptModal({ open: true, mode: 'edit', data: dept }); };
  const submitDept = async (e) => {
    e.preventDefault(); setFormError(''); setSubmitting(true);
    try {
      if (deptModal.mode === 'add') await API.post('/admin/departments', deptForm);
      else await API.put(`/admin/departments/${deptModal.data.id}`, deptForm);
      setFormSuccess(deptModal.mode === 'add' ? 'Department created!' : 'Department updated!');
      fetchAll(); setTimeout(() => setDeptModal({ open: false, mode: 'add', data: null }), 1000);
    } catch (err) { setFormError(err.response?.data || 'Operation failed'); }
    finally { setSubmitting(false); }
  };
  const deleteDept = async (id) => {
    if (!window.confirm('Delete this department? Users will be unassigned.')) return;
    try { await API.delete(`/admin/departments/${id}`); fetchAll(); }
    catch (err) { alert(err.response?.data || 'Delete failed'); }
  };

  // Leave Type CRUD
  const openAddLt = () => { setLtForm({ name: '', defaultDays: '', description: '' }); setFormError(''); setFormSuccess(''); setLtModal({ open: true, mode: 'add', data: null }); };
  const openEditLt = (lt) => { setLtForm({ name: lt.name, defaultDays: lt.defaultDays, description: lt.description || '' }); setFormError(''); setFormSuccess(''); setLtModal({ open: true, mode: 'edit', data: lt }); };
  const submitLt = async (e) => {
    e.preventDefault(); setFormError(''); setSubmitting(true);
    try {
      const payload = { ...ltForm, defaultDays: parseInt(ltForm.defaultDays) };
      if (ltModal.mode === 'add') await API.post('/admin/leave-types', payload);
      else await API.put(`/admin/leave-types/${ltModal.data.id}`, payload);
      setFormSuccess(ltModal.mode === 'add' ? 'Leave type created!' : 'Leave type updated!');
      fetchAll(); setTimeout(() => setLtModal({ open: false, mode: 'add', data: null }), 1000);
    } catch (err) { setFormError(err.response?.data || 'Operation failed'); }
    finally { setSubmitting(false); }
  };
  const deleteLt = async (id) => {
    if (!window.confirm('Delete this leave type? All balances will be removed.')) return;
    try { await API.delete(`/admin/leave-types/${id}`); fetchAll(); }
    catch (err) { alert(err.response?.data || 'Delete failed'); }
  };

  const closeModal = (setter) => () => { setter({ open: false, mode: 'add', data: null }); setFormError(''); setFormSuccess(''); };

  const FormFeedback = () => (
    <>
      {formError && <p className="msg msg-error">{formError}</p>}
      {formSuccess && <p className="msg msg-success">{formSuccess}</p>}
    </>
  );

  return (
    <div className="page">
      <header className="dash-header">
        <div>
          <strong>LeavePortal</strong>
          <span> — Admin Control Panel | {user?.username}</span>
        </div>
        <div className="header-actions">
          <button className="btn btn-secondary btn-small" onClick={fetchAll}>Refresh</button>
          <button className="btn btn-danger btn-small" onClick={handleLogout}>Logout</button>
        </div>
      </header>

      {/* Stats */}
      <div className="stats-row">
        {[
          { label: 'Employees', value: employees.filter(e => e.active).length },
          { label: 'Departments', value: departments.length },
          { label: 'Leave Types', value: leaveTypes.length },
          { label: 'Defaulters', value: defaulters.length },
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
          {[['reports','Reports'],['employees','Employees'],['departments','Departments'],['leavetypes','Leave Policies'],['defaulters','Defaulters']].map(([id, label]) => (
            <button key={id} className={`tab-btn ${activeTab === id ? 'active' : ''}`} onClick={() => setActiveTab(id)}>{label}</button>
          ))}
        </div>

        {/* REPORTS TAB */}
        {activeTab === 'reports' && (
          <div className="charts-grid">
            <div className="panel">
              <h3>Leave Usage by Type</h3>
              {leaveUsage.length > 0 ? (
                <ResponsiveContainer width="100%" height={240}>
                  <PieChart>
                    <Pie data={leaveUsage} dataKey="days" nameKey="name" cx="50%" cy="50%" outerRadius={80}
                      label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`} labelLine={false}>
                      {leaveUsage.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
                    </Pie>
                    <Tooltip />
                    <Legend wrapperStyle={{ fontSize: '0.8rem' }} />
                  </PieChart>
                </ResponsiveContainer>
              ) : <p className="empty-state">No leave data yet</p>}
            </div>
            <div className="panel">
              <h3>Leave Days by Department</h3>
              {deptStats.length > 0 ? (
                <ResponsiveContainer width="100%" height={240}>
                  <BarChart data={deptStats} margin={{ top: 5, right: 10, left: 0, bottom: 20 }}>
                    <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.05)" />
                    <XAxis dataKey="department" tick={{ fill: 'var(--text-secondary)', fontSize: 11 }} angle={-15} textAnchor="end" />
                    <YAxis tick={{ fill: 'var(--text-secondary)', fontSize: 11 }} />
                    <Tooltip />
                    <Bar dataKey="days" name="Days" fill="var(--color-success)" radius={[4, 4, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              ) : <p className="empty-state">No department data yet</p>}
            </div>
          </div>
        )}

        {/* EMPLOYEES TAB */}
        {activeTab === 'employees' && (
          <div className="panel">
            <div className="section-header">
              <h2>All Employees</h2>
              <button className="btn btn-primary btn-small" onClick={openAddEmp}>+ Add Employee</button>
            </div>
            <div className="table-container">
              <table>
                <thead>
                  <tr><th>Name</th><th>Username</th><th>Email</th><th>Role</th><th>Department</th><th>Manager</th><th>Status</th><th>Actions</th></tr>
                </thead>
                <tbody>
                  {employees.map(emp => (
                    <tr key={emp.id}>
                      <td><strong>{emp.firstName} {emp.lastName}</strong></td>
                      <td className="muted">@{emp.username}</td>
                      <td>{emp.email}</td>
                      <td><span className="badge">{emp.role}</span></td>
                      <td>{emp.department?.name || '-'}</td>
                      <td>{emp.manager ? `${emp.manager.firstName} ${emp.manager.lastName}` : '-'}</td>
                      <td><span className={`badge ${emp.active ? 'badge-approved' : 'badge-cancelled'}`}>{emp.active ? 'Active' : 'Inactive'}</span></td>
                      <td>
                        <div className="action-btns">
                          <button className="btn btn-secondary btn-small" onClick={() => openEditEmp(emp)}>Edit</button>
                          <button className="btn btn-danger btn-small" onClick={() => deleteEmp(emp.id)}>Delete</button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {/* DEPARTMENTS TAB */}
        {activeTab === 'departments' && (
          <div className="panel">
            <div className="section-header">
              <h2>Departments</h2>
              <button className="btn btn-primary btn-small" onClick={openAddDept}>+ Add Department</button>
            </div>
            <div className="cards-grid">
              {departments.map(dept => (
                <div key={dept.id} className="info-card">
                  <div>
                    <strong>{dept.name}</strong>
                    <p>{dept.description || 'No description'}</p>
                    <small className="muted">{employees.filter(e => e.department?.id === dept.id).length} employees</small>
                  </div>
                  <div className="action-btns">
                    <button className="btn btn-secondary btn-small" onClick={() => openEditDept(dept)}>Edit</button>
                    <button className="btn btn-danger btn-small" onClick={() => deleteDept(dept.id)}>Delete</button>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* LEAVE TYPES TAB */}
        {activeTab === 'leavetypes' && (
          <div className="panel">
            <div className="section-header">
              <h2>Leave Types &amp; Policies</h2>
              <button className="btn btn-primary btn-small" onClick={openAddLt}>+ Add Leave Type</button>
            </div>
            <div className="cards-grid">
              {leaveTypes.map((lt, idx) => (
                <div key={lt.id} className="info-card" style={{ borderLeft: `4px solid ${COLORS[idx % COLORS.length]}` }}>
                  <div>
                    <strong>{lt.name}</strong>
                    <p>{lt.description || 'No description'}</p>
                    <p><span style={{ fontSize: '1.4rem', fontWeight: 700, color: COLORS[idx % COLORS.length] }}>{lt.defaultDays}</span> days/year</p>
                  </div>
                  <div className="action-btns">
                    <button className="btn btn-secondary btn-small" onClick={() => openEditLt(lt)}>Edit</button>
                    <button className="btn btn-danger btn-small" onClick={() => deleteLt(lt.id)}>Delete</button>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* DEFAULTERS TAB */}
        {activeTab === 'defaulters' && (
          <div className="panel">
            <h2>Defaulter Report</h2>
            {defaulters.length === 0 ? (
              <p className="empty-state">No defaulters found! All employees are within their leave limits.</p>
            ) : (
              <div className="table-container">
                <table>
                  <thead>
                    <tr><th>Employee</th><th>Username</th><th>Department</th><th>Used Days</th><th>Reason</th></tr>
                  </thead>
                  <tbody>
                    {defaulters.map(d => (
                      <tr key={d.employeeId}>
                        <td><strong>{d.employeeName}</strong></td>
                        <td className="muted">@{d.username}</td>
                        <td>{d.department}</td>
                        <td style={{ color: 'var(--color-danger)', fontWeight: 700 }}>{d.usedDays}</td>
                        <td>{d.reason}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        )}
      </div>

      {/* Employee Modal */}
      {empModal.open && (
        <div className="modal-overlay">
          <div className="modal-content" style={{ maxWidth: 540, maxHeight: '90vh', overflowY: 'auto' }}>
            <div className="modal-header">
              <h3>{empModal.mode === 'add' ? 'Add Employee' : 'Edit Employee'}</h3>
              <button className="close-btn" onClick={closeModal(setEmpModal)}>✕</button>
            </div>
            <form onSubmit={submitEmp} className="modal-body">
              <FormFeedback />
              <div className="row-2">
                <div><label>First Name</label><input value={empForm.firstName} onChange={e => setEmpForm({ ...empForm, firstName: e.target.value })} required /></div>
                <div><label>Last Name</label><input value={empForm.lastName} onChange={e => setEmpForm({ ...empForm, lastName: e.target.value })} required /></div>
              </div>
              <label>Email</label><input type="email" value={empForm.email} onChange={e => setEmpForm({ ...empForm, email: e.target.value })} required />
              {empModal.mode === 'add' && (
                <>
                  <label>Username</label><input value={empForm.username} onChange={e => setEmpForm({ ...empForm, username: e.target.value })} required />
                  <label>Password</label><input type="password" value={empForm.password} onChange={e => setEmpForm({ ...empForm, password: e.target.value })} required minLength={6} />
                </>
              )}
              <label>Role</label>
              <select value={empForm.role} onChange={e => setEmpForm({ ...empForm, role: e.target.value })}>
                <option value="EMPLOYEE">Employee</option>
                <option value="MANAGER">Manager</option>
                <option value="ADMIN">Admin</option>
              </select>
              <label>Department</label>
              <select value={empForm.departmentId} onChange={e => setEmpForm({ ...empForm, departmentId: e.target.value })}>
                <option value="">-- No Department --</option>
                {departments.map(d => <option key={d.id} value={d.id}>{d.name}</option>)}
              </select>
              <label>Manager (Reporting To)</label>
              <select value={empForm.managerId} onChange={e => setEmpForm({ ...empForm, managerId: e.target.value })}>
                <option value="">-- No Manager --</option>
                {employees.filter(e => e.role === 'MANAGER' || e.role === 'ADMIN').map(e => <option key={e.id} value={e.id}>{e.firstName} {e.lastName} ({e.role})</option>)}
              </select>
              {empModal.mode === 'edit' && (
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <input type="checkbox" id="activeCheck" checked={empForm.active !== false} onChange={e => setEmpForm({ ...empForm, active: e.target.checked })} style={{ width: 'auto' }} />
                  <label htmlFor="activeCheck" style={{ margin: 0 }}>Active</label>
                </div>
              )}
              <div className="modal-footer">
                <button type="button" className="btn btn-secondary btn-small" onClick={closeModal(setEmpModal)}>Cancel</button>
                <button type="submit" className="btn btn-primary btn-small" disabled={submitting}>{submitting ? 'Saving...' : 'Save'}</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Department Modal */}
      {deptModal.open && (
        <div className="modal-overlay">
          <div className="modal-content">
            <div className="modal-header">
              <h3>{deptModal.mode === 'add' ? 'Add Department' : 'Edit Department'}</h3>
              <button className="close-btn" onClick={closeModal(setDeptModal)}>✕</button>
            </div>
            <form onSubmit={submitDept} className="modal-body">
              <FormFeedback />
              <label>Department Name</label><input value={deptForm.name} onChange={e => setDeptForm({ ...deptForm, name: e.target.value })} required placeholder="e.g. Engineering" />
              <label>Description</label><textarea rows="3" value={deptForm.description} onChange={e => setDeptForm({ ...deptForm, description: e.target.value })} placeholder="Optional..." />
              <div className="modal-footer">
                <button type="button" className="btn btn-secondary btn-small" onClick={closeModal(setDeptModal)}>Cancel</button>
                <button type="submit" className="btn btn-primary btn-small" disabled={submitting}>{submitting ? 'Saving...' : 'Save'}</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Leave Type Modal */}
      {ltModal.open && (
        <div className="modal-overlay">
          <div className="modal-content">
            <div className="modal-header">
              <h3>{ltModal.mode === 'add' ? 'Add Leave Type' : 'Edit Leave Type'}</h3>
              <button className="close-btn" onClick={closeModal(setLtModal)}>✕</button>
            </div>
            <form onSubmit={submitLt} className="modal-body">
              <FormFeedback />
              <label>Leave Type Name</label><input value={ltForm.name} onChange={e => setLtForm({ ...ltForm, name: e.target.value })} required placeholder="e.g. Sick Leave" />
              <label>Default Days Per Year</label><input type="number" min="1" value={ltForm.defaultDays} onChange={e => setLtForm({ ...ltForm, defaultDays: e.target.value })} required placeholder="e.g. 12" />
              <label>Description</label><textarea rows="3" value={ltForm.description} onChange={e => setLtForm({ ...ltForm, description: e.target.value })} placeholder="Optional..." />
              <div className="modal-footer">
                <button type="button" className="btn btn-secondary btn-small" onClick={closeModal(setLtModal)}>Cancel</button>
                <button type="submit" className="btn btn-primary btn-small" disabled={submitting}>{submitting ? 'Saving...' : 'Save'}</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}