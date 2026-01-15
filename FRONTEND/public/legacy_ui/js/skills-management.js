/**
 * User Skills Management Module
 * Handles skill types and user skill assignments for skill-based case routing
 */

// Skills API base path
const SKILLS_API = 'api/v1/skills';

// State management
let skillTypesCache = [];
let currentUserSkills = {};

// ==================== Skill Type Functions ====================

/**
 * Fetch all active skill types
 */
async function fetchSkillTypes() {
    try {
        const response = await fetch(`${SKILLS_API}/types`, getFetchOptions());
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        skillTypesCache = await response.json();
        return skillTypesCache;
    } catch (error) {
        console.error('Error fetching skill types:', error);
        handleApiError(error, 'fetchSkillTypes');
        return [];
    }
}

/**
 * Create new skill type
 */
async function createSkillType(name, description, caseType) {
    try {
        const response = await fetch(`${SKILLS_API}/types`, getFetchOptions('POST', {
            name,
            description,
            caseType
        }));
        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            throw new Error(errorData.message || `HTTP ${response.status}`);
        }
        const skillType = await response.json();
        showNotification('Skill type created successfully', 'success');
        await fetchSkillTypes(); // Refresh cache
        return skillType;
    } catch (error) {
        console.error('Error creating skill type:', error);
        handleApiError(error, 'createSkillType');
        throw error;
    }
}

/**
 * Update skill type
 */
async function updateSkillType(id, description, caseType, active) {
    try {
        const response = await fetch(`${SKILLS_API}/types/${id}`, getFetchOptions('PUT', {
            description,
            caseType,
            active
        }));
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        const skillType = await response.json();
        showNotification('Skill type updated successfully', 'success');
        await fetchSkillTypes(); // Refresh cache
        return skillType;
    } catch (error) {
        console.error('Error updating skill type:', error);
        handleApiError(error, 'updateSkillType');
        throw error;
    }
}

/**
 * Deactivate skill type
 */
async function deactivateSkillType(id) {
    try {
        const response = await fetch(`${SKILLS_API}/types/${id}`, getFetchOptions('DELETE'));
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        showNotification('Skill type deactivated', 'success');
        await fetchSkillTypes(); // Refresh cache
    } catch (error) {
        console.error('Error deactivating skill type:', error);
        handleApiError(error, 'deactivateSkillType');
        throw error;
    }
}

// ==================== User Skills Functions ====================

/**
 * Fetch skills for a specific user
 */
async function fetchUserSkills(userId) {
    try {
        const response = await fetch(`${SKILLS_API}/users/${userId}`, getFetchOptions());
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        const skills = await response.json();
        currentUserSkills[userId] = skills;
        return skills;
    } catch (error) {
        console.error('Error fetching user skills:', error);
        handleApiError(error, 'fetchUserSkills');
        return [];
    }
}

/**
 * Add skill to user
 */
async function addSkillToUser(userId, skillTypeId, proficiencyLevel) {
    try {
        const response = await fetch(`${SKILLS_API}/users/${userId}`, getFetchOptions('POST', {
            skillTypeId,
            proficiencyLevel: proficiencyLevel || 1
        }));
        if (!response.ok) {
            const errorData = await response.json().catch(() => ({}));
            throw new Error(errorData.message || `HTTP ${response.status}`);
        }
        const userSkill = await response.json();
        showNotification('Skill added successfully', 'success');
        await fetchUserSkills(userId); // Refresh user's skills
        return userSkill;
    } catch (error) {
        console.error('Error adding skill to user:', error);
        handleApiError(error, 'addSkillToUser');
        throw error;
    }
}

/**
 * Update user skill
 */
async function updateUserSkillLevel(userId, skillId, proficiencyLevel, notes) {
    try {
        const response = await fetch(`${SKILLS_API}/users/${userId}/skills/${skillId}`, getFetchOptions('PUT', {
            proficiencyLevel,
            notes
        }));
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        const userSkill = await response.json();
        showNotification('Skill updated successfully', 'success');
        await fetchUserSkills(userId); // Refresh user's skills
        return userSkill;
    } catch (error) {
        console.error('Error updating user skill:', error);
        handleApiError(error, 'updateUserSkillLevel');
        throw error;
    }
}

/**
 * Remove skill from user
 */
async function removeSkillFromUser(userId, skillTypeId) {
    try {
        const response = await fetch(`${SKILLS_API}/users/${userId}/skills/${skillTypeId}`, getFetchOptions('DELETE'));
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        showNotification('Skill removed successfully', 'success');
        await fetchUserSkills(userId); // Refresh user's skills
    } catch (error) {
        console.error('Error removing skill from user:', error);
        handleApiError(error, 'removeSkillFromUser');
        throw error;
    }
}

/**
 * Certify a user skill
 */
async function certifyUserSkill(userSkillId, expiresAt) {
    try {
        const response = await fetch(`${SKILLS_API}/users/skills/${userSkillId}/certify`, getFetchOptions('POST', {
            expiresAt
        }));
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        const userSkill = await response.json();
        showNotification('Skill certified successfully', 'success');
        return userSkill;
    } catch (error) {
        console.error('Error certifying skill:', error);
        handleApiError(error, 'certifyUserSkill');
        throw error;
    }
}

// ==================== UI Rendering Functions ====================

/**
 * Render skill badges for a user
 */
function renderSkillBadges(skills) {
    if (!skills || skills.length === 0) {
        return '<span class="no-skills">No skills assigned</span>';
    }

    return skills.map(skill => {
        const levelColors = {
            1: '#95a5a6', // Novice - Gray
            2: '#3498db', // Beginner - Blue
            3: '#f39c12', // Intermediate - Orange
            4: '#27ae60', // Advanced - Green
            5: '#9b59b6'  // Expert - Purple
        };
        const color = levelColors[skill.proficiencyLevel] || '#7f8c8d';
        const certifiedBadge = skill.certified ? '<i class="fas fa-certificate" style="color: gold; margin-left: 4px;" title="Certified"></i>' : '';

        return `
            <span class="skill-badge" style="background-color: ${color}; color: white; padding: 2px 8px; border-radius: 12px; font-size: 0.75rem; margin-right: 4px;">
                ${skill.skillTypeName} (L${skill.proficiencyLevel})${certifiedBadge}
            </span>
        `;
    }).join('');
}

/**
 * Render proficiency level selector
 */
function renderProficiencySelector(currentLevel = 1, inputName = 'proficiencyLevel') {
    const levels = [
        { value: 1, label: 'Novice' },
        { value: 2, label: 'Beginner' },
        { value: 3, label: 'Intermediate' },
        { value: 4, label: 'Advanced' },
        { value: 5, label: 'Expert' }
    ];

    return `
        <select name="${inputName}" class="form-control">
            ${levels.map(level => `
                <option value="${level.value}" ${level.value === currentLevel ? 'selected' : ''}>
                    ${level.value} - ${level.label}
                </option>
            `).join('')}
        </select>
    `;
}

/**
 * Render skill types dropdown
 */
function renderSkillTypeDropdown(selectedId = null, inputName = 'skillTypeId', excludeIds = []) {
    const availableSkills = skillTypesCache.filter(s => !excludeIds.includes(s.id));

    return `
        <select name="${inputName}" class="form-control" required>
            <option value="">Select a skill...</option>
            ${availableSkills.map(skill => `
                <option value="${skill.id}" ${skill.id === selectedId ? 'selected' : ''}>
                    ${skill.name.replace(/_/g, ' ')}
                </option>
            `).join('')}
        </select>
    `;
}

/**
 * Open user skills modal
 */
async function openUserSkillsModal(userId, username) {
    // Ensure skill types are loaded
    if (skillTypesCache.length === 0) {
        await fetchSkillTypes();
    }

    // Fetch user's current skills
    const userSkills = await fetchUserSkills(userId);
    const existingSkillIds = userSkills.map(s => s.skillTypeId);

    const modal = document.getElementById('userSkillsModal');
    if (!modal) {
        console.error('User skills modal not found');
        return;
    }

    // Populate modal content
    document.getElementById('skillsModalTitle').textContent = `Skills for ${username}`;
    document.getElementById('skillsUserId').value = userId;

    // Render current skills
    const skillsListContainer = document.getElementById('userSkillsList');
    if (userSkills.length === 0) {
        skillsListContainer.innerHTML = '<p class="text-muted">No skills assigned yet.</p>';
    } else {
        skillsListContainer.innerHTML = `
            <table class="table table-sm">
                <thead>
                    <tr>
                        <th>Skill</th>
                        <th>Level</th>
                        <th>Certified</th>
                        <th>Actions</th>
                    </tr>
                </thead>
                <tbody>
                    ${userSkills.map(skill => `
                        <tr data-skill-id="${skill.id}" data-skill-type-id="${skill.skillTypeId}">
                            <td>${skill.skillTypeName.replace(/_/g, ' ')}</td>
                            <td>
                                <select class="form-control form-control-sm skill-level-select" data-skill-id="${skill.id}">
                                    ${[1, 2, 3, 4, 5].map(l => `
                                        <option value="${l}" ${l === skill.proficiencyLevel ? 'selected' : ''}>
                                            ${l} - ${skill.proficiencyDescription || getProficiencyLabel(l)}
                                        </option>
                                    `).join('')}
                                </select>
                            </td>
                            <td>
                                ${skill.certified
                ? '<span class="badge badge-success"><i class="fas fa-certificate"></i> Certified</span>'
                : '<span class="badge badge-secondary">Not certified</span>'
            }
                            </td>
                            <td>
                                <button class="btn btn-sm btn-primary" onclick="updateSkillFromModal(${userId}, ${skill.id})" title="Save">
                                    <i class="fas fa-save"></i>
                                </button>
                                <button class="btn btn-sm btn-danger" onclick="removeSkillFromModal(${userId}, ${skill.skillTypeId})" title="Remove">
                                    <i class="fas fa-trash"></i>
                                </button>
                            </td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        `;
    }

    // Render add new skill form
    const addSkillForm = document.getElementById('addUserSkillForm');
    addSkillForm.innerHTML = `
        <div class="form-row">
            <div class="form-group col-md-6">
                <label>Skill Type</label>
                ${renderSkillTypeDropdown(null, 'newSkillTypeId', existingSkillIds)}
            </div>
            <div class="form-group col-md-4">
                <label>Proficiency Level</label>
                ${renderProficiencySelector(1, 'newProficiencyLevel')}
            </div>
            <div class="form-group col-md-2 d-flex align-items-end">
                <button type="button" class="btn btn-success" onclick="addSkillFromModal(${userId})">
                    <i class="fas fa-plus"></i> Add
                </button>
            </div>
        </div>
    `;

    // Show modal
    modal.style.display = 'block';
}

/**
 * Close user skills modal
 */
function closeUserSkillsModal() {
    const modal = document.getElementById('userSkillsModal');
    if (modal) {
        modal.style.display = 'none';
    }
}

/**
 * Add skill from modal
 */
async function addSkillFromModal(userId) {
    const skillTypeId = document.querySelector('[name="newSkillTypeId"]').value;
    const proficiencyLevel = parseInt(document.querySelector('[name="newProficiencyLevel"]').value);

    if (!skillTypeId) {
        showNotification('Please select a skill type', 'warning');
        return;
    }

    try {
        await addSkillToUser(userId, parseInt(skillTypeId), proficiencyLevel);
        // Refresh modal content
        const username = document.getElementById('skillsModalTitle').textContent.replace('Skills for ', '');
        await openUserSkillsModal(userId, username);
    } catch (error) {
        // Error already handled in addSkillToUser
    }
}

/**
 * Update skill from modal
 */
async function updateSkillFromModal(userId, skillId) {
    const levelSelect = document.querySelector(`.skill-level-select[data-skill-id="${skillId}"]`);
    if (!levelSelect) return;

    const proficiencyLevel = parseInt(levelSelect.value);

    try {
        await updateUserSkillLevel(userId, skillId, proficiencyLevel, null);
    } catch (error) {
        // Error already handled
    }
}

/**
 * Remove skill from modal
 */
async function removeSkillFromModal(userId, skillTypeId) {
    if (!confirm('Are you sure you want to remove this skill?')) return;

    try {
        await removeSkillFromUser(userId, skillTypeId);
        // Refresh modal content
        const username = document.getElementById('skillsModalTitle').textContent.replace('Skills for ', '');
        await openUserSkillsModal(userId, username);
    } catch (error) {
        // Error already handled
    }
}

/**
 * Get proficiency label
 */
function getProficiencyLabel(level) {
    const labels = {
        1: 'Novice',
        2: 'Beginner',
        3: 'Intermediate',
        4: 'Advanced',
        5: 'Expert'
    };
    return labels[level] || 'Unknown';
}

/**
 * Show notification (compatibility with existing dashboard.js)
 */
function showNotification(message, type = 'info') {
    // Use existing notification system if available
    if (typeof window.showToast === 'function') {
        window.showToast(message, type);
    } else if (typeof alert !== 'undefined') {
        // Fallback to console for non-blocking
        console.log(`[${type.toUpperCase()}] ${message}`);
        // For important messages, show alert
        if (type === 'error' || type === 'warning') {
            alert(message);
        }
    }
}

// ==================== Export Functions to Window ====================

// Make functions globally available
window.fetchSkillTypes = fetchSkillTypes;
window.createSkillType = createSkillType;
window.updateSkillType = updateSkillType;
window.deactivateSkillType = deactivateSkillType;
window.fetchUserSkills = fetchUserSkills;
window.addSkillToUser = addSkillToUser;
window.updateUserSkillLevel = updateUserSkillLevel;
window.removeSkillFromUser = removeSkillFromUser;
window.certifyUserSkill = certifyUserSkill;
window.renderSkillBadges = renderSkillBadges;
window.openUserSkillsModal = openUserSkillsModal;
window.closeUserSkillsModal = closeUserSkillsModal;
window.addSkillFromModal = addSkillFromModal;
window.updateSkillFromModal = updateSkillFromModal;
window.removeSkillFromModal = removeSkillFromModal;

// Initialize on load
document.addEventListener('DOMContentLoaded', function () {
    // Pre-fetch skill types for faster modal opening
    fetchSkillTypes().catch(err => console.warn('Could not pre-fetch skill types:', err));
});
