const state = {
  campaignKey: null,
  campaigns: [],
  nodes: [],
  choices: [],
  positions: {},
  selectedNodeKey: null,
  drag: null,
  connectMode: null,
  viewport: {
    x: 0,
    y: 0,
    scale: 1,
    pan: null,
  },
};

const canvas = document.getElementById('canvas');
const connections = document.getElementById('connections');
const viewport = document.getElementById('viewport');
const nodeList = document.getElementById('nodeList');
const campaignSelect = document.getElementById('campaignSelect');
const preview = document.getElementById('preview');
const inspector = {
  title: document.getElementById('nodeTitle'),
  key: document.getElementById('nodeKey'),
  variants: document.getElementById('nodeVariants'),
  terminal: document.getElementById('nodeTerminal'),
  auto: document.getElementById('nodeAuto'),
  reward: document.getElementById('nodeReward'),
  choices: document.getElementById('choiceList'),
};

async function api(path, options = {}) {
  const response = await fetch(path, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  });
  if (!response.ok) {
    const error = await response.text();
    throw new Error(error || 'Ошибка запроса');
  }
  if (response.status === 204) {
    return null;
  }
  return response.json();
}

async function loadCampaigns() {
  const campaigns = await api('/api/story/campaigns');
  state.campaigns = campaigns;
  campaignSelect.innerHTML = '';
  campaigns.forEach(c => {
    const option = document.createElement('option');
    option.value = c.campaignKey;
    option.textContent = `${c.name} (${c.campaignKey})`;
    campaignSelect.appendChild(option);
  });
  if (campaigns.length) {
    state.campaignKey = campaigns[0].campaignKey;
    campaignSelect.value = state.campaignKey;
  }
}

async function loadGraph() {
  if (!state.campaignKey) return;
  const graph = await api(`/api/story/${state.campaignKey}/graph`);
  state.nodes = graph.nodes;
  state.choices = graph.choices;
  const storedPositions = localStorage.getItem(`story-pos-${state.campaignKey}`);
  state.positions = JSON.parse(storedPositions || '{}');
  state.nodes.forEach((node, index) => {
    if (!state.positions[node.nodeKey]) {
      state.positions[node.nodeKey] = { x: 160 + index * 40, y: 140 + index * 30 };
    }
  });
  if (!storedPositions) {
    applyAutoLayout();
  }
  state.selectedNodeKey = state.nodes[0]?.nodeKey ?? null;
  render();
}

function savePositions() {
  if (!state.campaignKey) return;
  localStorage.setItem(`story-pos-${state.campaignKey}`, JSON.stringify(state.positions));
}

function render() {
  renderNodeList();
  renderNodes();
  renderConnections();
  renderInspector();
  renderPreview();
}

function renderNodeList() {
  nodeList.innerHTML = '';
  state.nodes.forEach(node => {
    const btn = document.createElement('button');
    btn.textContent = node.title;
    if (node.nodeKey === state.selectedNodeKey) {
      btn.classList.add('active');
    }
    btn.onclick = () => selectNode(node.nodeKey);
    nodeList.appendChild(btn);
  });
}

function renderNodes() {
  viewport.querySelectorAll('.node').forEach(node => node.remove());
  state.nodes.forEach(node => {
    const pos = state.positions[node.nodeKey];
    const el = document.createElement('div');
    el.className = 'node';
    el.dataset.nodeKey = node.nodeKey;
    el.style.left = `${pos.x}px`;
    el.style.top = `${pos.y}px`;
    if (node.nodeKey === state.selectedNodeKey) {
      el.style.borderColor = 'var(--node-active)';
    }

    const choiceCount = state.choices.filter(c => c.nodeKey === node.nodeKey).length;

    el.innerHTML = `
      <div class="node-header">
        <div>${node.title}</div>
        <span class="badge">${node.terminalType}</span>
      </div>
      <div class="node-body">
        <div>${(getVariantLines(node).join(' ') || '').slice(0, 90)}</div>
        <div class="node-socket input"><div class="dot"></div>Вход</div>
        <div class="node-socket output"><div class="dot"></div>Выходы: ${choiceCount}</div>
      </div>
    `;

    el.addEventListener('pointerdown', (event) => {
      if (event.target.closest('.connector-btn')) return;
      state.drag = {
        nodeKey: node.nodeKey,
        startX: event.clientX,
        startY: event.clientY,
        originX: pos.x,
        originY: pos.y,
      };
      el.setPointerCapture(event.pointerId);
    });

    el.addEventListener('pointerup', () => {
      state.drag = null;
      savePositions();
    });

    el.onclick = (event) => {
      if (state.connectMode) {
        applyConnection(node.nodeKey);
        return;
      }
      if (!event.defaultPrevented) {
        selectNode(node.nodeKey);
      }
    };

    viewport.appendChild(el);
  });
}

function renderConnections() {
  connections.innerHTML = '';
  const bounds = canvas.getBoundingClientRect();
  const nodeWidth = 260;
  const nodeHeight = 140;
  const extents = Object.values(state.positions).reduce((acc, pos) => {
    acc.maxX = Math.max(acc.maxX, pos.x + nodeWidth + 40);
    acc.maxY = Math.max(acc.maxY, pos.y + nodeHeight + 40);
    return acc;
  }, { maxX: bounds.width, maxY: bounds.height });
  const width = Math.max(bounds.width, extents.maxX);
  const height = Math.max(bounds.height, extents.maxY);
  connections.setAttribute('width', width);
  connections.setAttribute('height', height);
  connections.setAttribute('viewBox', `0 0 ${width} ${height}`);
  const drawEdge = (sourceKey, targetKey, color, dash) => {
    const source = state.positions[sourceKey];
    const target = state.positions[targetKey];
    if (!source || !target) return;
    const path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
    const startX = source.x + nodeWidth;
    const startY = source.y + nodeHeight / 2;
    const endX = target.x;
    const endY = target.y + nodeHeight / 2;
    const midX = (startX + endX) / 2;
    path.setAttribute('d', `M ${startX} ${startY} C ${midX} ${startY}, ${midX} ${endY}, ${endX} ${endY}`);
    path.setAttribute('stroke', color);
    path.setAttribute('stroke-width', '2');
    if (dash) {
      path.setAttribute('stroke-dasharray', dash);
    }
    path.setAttribute('fill', 'none');
    connections.appendChild(path);
  };
  state.choices.forEach(choice => {
    if (choice.successNodeKey) {
      drawEdge(choice.nodeKey, choice.successNodeKey, 'rgba(111, 157, 255, 0.8)');
    }
    if (choice.failNodeKey) {
      drawEdge(choice.nodeKey, choice.failNodeKey, 'rgba(255, 107, 107, 0.75)', '6 6');
    }
  });
}

function updateViewportTransform() {
  viewport.style.transform = `translate(${state.viewport.x}px, ${state.viewport.y}px) scale(${state.viewport.scale})`;
}

function applyAutoLayout() {
  if (!state.nodes.length) return;
  const campaign = state.campaigns?.find(c => c.campaignKey === state.campaignKey);
  const startKey = campaign?.startNodeKey ?? state.nodes[0].nodeKey;
  const adjacency = new Map();
  state.nodes.forEach(node => adjacency.set(node.nodeKey, []));
  state.choices.forEach(choice => {
    if (choice.successNodeKey) adjacency.get(choice.nodeKey)?.push(choice.successNodeKey);
    if (choice.failNodeKey) adjacency.get(choice.nodeKey)?.push(choice.failNodeKey);
  });

  const levels = new Map();
  const queue = [startKey];
  levels.set(startKey, 0);
  while (queue.length) {
    const current = queue.shift();
    const level = levels.get(current) ?? 0;
    (adjacency.get(current) || []).forEach(next => {
      if (!levels.has(next)) {
        levels.set(next, level + 1);
        queue.push(next);
      }
    });
  }

  const terminals = state.nodes.filter(n => n.terminalType !== 'NONE').map(n => n.nodeKey);
  const maxLevel = Math.max(0, ...levels.values());
  terminals.forEach(key => levels.set(key, Math.max(levels.get(key) ?? maxLevel, maxLevel + 1)));
  state.nodes.forEach(node => {
    if (!levels.has(node.nodeKey)) {
      levels.set(node.nodeKey, maxLevel + 1);
    }
  });

  const groups = {};
  levels.forEach((level, key) => {
    if (!groups[level]) groups[level] = [];
    groups[level].push(key);
  });

  Object.entries(groups).forEach(([level, keys]) => {
    keys.sort((a, b) => {
      const outA = (adjacency.get(a) || []).length;
      const outB = (adjacency.get(b) || []).length;
      if (outA !== outB) return outB - outA;
      return a.localeCompare(b);
    });
    keys.forEach((key, idx) => {
      state.positions[key] = {
        x: 140 + Number(level) * 320,
        y: 120 + idx * 180,
      };
    });
  });
  savePositions();
  render();
}

function renderInspector() {
  const node = state.nodes.find(n => n.nodeKey === state.selectedNodeKey);
  if (!node) {
    inspector.title.value = '';
    inspector.key.value = '';
    inspector.variants.value = '';
    inspector.terminal.value = 'NONE';
    inspector.auto.value = '';
    inspector.reward.value = '';
    inspector.choices.innerHTML = '<div class="badge">Выберите узел</div>';
    return;
  }
  inspector.title.value = node.title;
  inspector.key.value = node.nodeKey;
  inspector.variants.value = getVariantLines(node).join('\n');
  inspector.terminal.value = node.terminalType;
  inspector.auto.value = node.autoEffectsJson || '';
  inspector.reward.value = node.rewardJson || '';

  const choices = state.choices.filter(c => c.nodeKey === node.nodeKey);
  inspector.choices.innerHTML = '';
  choices.forEach(choice => {
    const wrapper = document.createElement('div');
    wrapper.className = 'choice-card';
    wrapper.innerHTML = `
      <h4>${choice.label || 'Выбор'}</h4>
      <div class="field">
        <label>Текст кнопки</label>
        <input value="${choice.label || ''}" data-choice-id="${choice.choiceKey}" data-field="label" />
      </div>
      <div class="field">
        <label>Условия (JSON)</label>
        <textarea data-choice-id="${choice.choiceKey}" data-field="conditionsJson" placeholder='{"min_cash":100}'>${choice.conditionsJson || ''}</textarea>
      </div>
      <div class="field">
        <label>Проверка (JSON)</label>
        <textarea data-choice-id="${choice.choiceKey}" data-field="checkJson" placeholder='{"type":"roll","stat":"discipline","base_success":0.55}'>${choice.checkJson || ''}</textarea>
      </div>
      <div class="field">
        <label>Успешный узел</label>
        <div class="choice-actions">
          <select data-choice-id="${choice.choiceKey}" data-field="successNodeKey">
            ${renderNodeOptions(choice.successNodeKey)}
          </select>
          <button class="connector-btn" data-choice-id="${choice.choiceKey}" data-target="success">+</button>
        </div>
      </div>
      <div class="field">
        <label>Провальный узел</label>
        <div class="choice-actions">
          <select data-choice-id="${choice.choiceKey}" data-field="failNodeKey">
            ${renderNodeOptions(choice.failNodeKey)}
          </select>
          <button class="connector-btn" data-choice-id="${choice.choiceKey}" data-target="fail">+</button>
        </div>
      </div>
      <div class="field">
        <label>Эффекты успеха (JSON)</label>
        <textarea data-choice-id="${choice.choiceKey}" data-field="successEffectsJson" placeholder='[{"op":"stat.add","key":"cash","value":50}]'>${choice.successEffectsJson || ''}</textarea>
      </div>
      <div class="field">
        <label>Эффекты провала (JSON)</label>
        <textarea data-choice-id="${choice.choiceKey}" data-field="failEffectsJson" placeholder='[{"op":"stat.add","key":"hp","value":-5}]'>${choice.failEffectsJson || ''}</textarea>
      </div>
    `;
    inspector.choices.appendChild(wrapper);
  });
}

function renderNodeOptions(selected) {
  return ['<option value="">— не выбран —</option>']
    .concat(state.nodes.map(node => {
      const isSelected = node.nodeKey === selected ? 'selected' : '';
      return `<option value="${node.nodeKey}" ${isSelected}>${node.title}</option>`;
    }))
    .join('');
}

function getVariantLines(node) {
  if (!node.variantsJson) return [];
  try {
    const parsed = JSON.parse(node.variantsJson);
    if (Array.isArray(parsed)) {
      return parsed.map(v => String(v));
    }
  } catch (e) {
    return node.variantsJson.split('\n').map(v => v.trim()).filter(Boolean);
  }
  return [];
}

function renderPreview() {
  const node = state.nodes.find(n => n.nodeKey === state.selectedNodeKey);
  if (!node) {
    preview.innerHTML = '<div class="badge">Нет данных</div>';
    return;
  }
  const choiceButtons = state.choices.filter(c => c.nodeKey === node.nodeKey)
    .map(c => `<div class="button">${c.label || '...'}</div>`)
    .join('');
  const text = getVariantLines(node)[0] || 'Текст узла';
  preview.innerHTML = `
    <div class="title">${node.title}</div>
    <div>${text}</div>
    <div class="hud">❤️ 100 | 💵 $0 | 🚨 0 | 🪙 0</div>
    <div class="buttons">${choiceButtons}</div>
  `;
}

function selectNode(nodeKey) {
  state.selectedNodeKey = nodeKey;
  state.connectMode = null;
  render();
}

function applyConnection(targetNodeKey) {
  const { choiceKey, targetType } = state.connectMode;
  const choice = state.choices.find(c => c.choiceKey === choiceKey);
  if (!choice) return;
  if (targetType === 'success') {
    choice.successNodeKey = targetNodeKey;
  } else {
    choice.failNodeKey = targetNodeKey;
  }
  updateChoice(choice);
  state.connectMode = null;
}

async function updateNode(node) {
  await api(`/api/story/${state.campaignKey}/nodes/${node.nodeKey}`, {
    method: 'PUT',
    body: JSON.stringify(node),
  });
}

async function updateChoice(choice) {
  await api(`/api/story/${state.campaignKey}/choices/${choice.choiceKey}`, {
    method: 'PUT',
    body: JSON.stringify(choice),
  });
  render();
}

async function createNode() {
  const nodeKey = `N_${Date.now().toString(36)}`.toUpperCase();
  const payload = {
    nodeKey,
    title: 'Новый узел',
    variantsJson: JSON.stringify(['Новый текст']),
    autoEffectsJson: '{}',
    terminalType: 'NONE',
    rewardJson: '{}',
    terminal: false,
  };
  const newNode = await api(`/api/story/${state.campaignKey}/nodes`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
  state.nodes.push(newNode);
  state.positions[newNode.nodeKey] = { x: 160, y: 160 };
  selectNode(newNode.nodeKey);
}

async function createChoice() {
  const node = state.nodes.find(n => n.nodeKey === state.selectedNodeKey);
  if (!node) return;
  const payload = {
    nodeKey: node.nodeKey,
    choiceKey: `C_${Date.now().toString(36)}`.toUpperCase(),
    label: 'Новый выбор',
    sortOrder: state.choices.filter(c => c.nodeKey === node.nodeKey).length + 1,
    conditionsJson: '{}',
    checkJson: '{"type":"none"}',
    successNodeKey: node.nodeKey,
    failNodeKey: node.nodeKey,
    successText: 'Успех',
    failText: 'Провал',
    successEffectsJson: '[]',
    failEffectsJson: '[]',
  };
  const newChoice = await api(`/api/story/${state.campaignKey}/choices`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
  state.choices.push(newChoice);
  render();
}

canvas.addEventListener('pointerdown', (event) => {
  if (event.button !== 1) return;
  state.viewport.pan = {
    startX: event.clientX,
    startY: event.clientY,
    originX: state.viewport.x,
    originY: state.viewport.y,
  };
  canvas.setPointerCapture(event.pointerId);
});

canvas.addEventListener('pointermove', (event) => {
  if (state.viewport.pan) {
    state.viewport.x = state.viewport.pan.originX + (event.clientX - state.viewport.pan.startX);
    state.viewport.y = state.viewport.pan.originY + (event.clientY - state.viewport.pan.startY);
    updateViewportTransform();
    return;
  }
  if (!state.drag) return;
  const pos = state.positions[state.drag.nodeKey];
  pos.x = state.drag.originX + (event.clientX - state.drag.startX) / state.viewport.scale;
  pos.y = state.drag.originY + (event.clientY - state.drag.startY) / state.viewport.scale;
  render();
});

canvas.addEventListener('pointerup', () => {
  state.drag = null;
  state.viewport.pan = null;
  savePositions();
});

canvas.addEventListener('pointerleave', () => {
  state.drag = null;
  state.viewport.pan = null;
});

canvas.addEventListener('wheel', (event) => {
  event.preventDefault();
  const bounds = canvas.getBoundingClientRect();
  const mouseX = event.clientX - bounds.left;
  const mouseY = event.clientY - bounds.top;
  const scaleDelta = event.deltaY < 0 ? 1.1 : 0.9;
  const nextScale = Math.min(2, Math.max(0.4, state.viewport.scale * scaleDelta));
  const worldX = (mouseX - state.viewport.x) / state.viewport.scale;
  const worldY = (mouseY - state.viewport.y) / state.viewport.scale;
  state.viewport.x = mouseX - worldX * nextScale;
  state.viewport.y = mouseY - worldY * nextScale;
  state.viewport.scale = nextScale;
  updateViewportTransform();
}, { passive: false });

inspector.title.addEventListener('input', async () => {
  const node = state.nodes.find(n => n.nodeKey === state.selectedNodeKey);
  if (!node) return;
  node.title = inspector.title.value;
  renderNodeList();
  await updateNode(node);
});

inspector.variants.addEventListener('input', async () => {
  const node = state.nodes.find(n => n.nodeKey === state.selectedNodeKey);
  if (!node) return;
  const lines = inspector.variants.value.split('\n').map(v => v.trim()).filter(Boolean);
  node.variantsJson = JSON.stringify(lines);
  await updateNode(node);
  renderPreview();
});

inspector.auto.addEventListener('input', async () => {
  const node = state.nodes.find(n => n.nodeKey === state.selectedNodeKey);
  if (!node) return;
  node.autoEffectsJson = inspector.auto.value;
  await updateNode(node);
});

inspector.reward.addEventListener('input', async () => {
  const node = state.nodes.find(n => n.nodeKey === state.selectedNodeKey);
  if (!node) return;
  node.rewardJson = inspector.reward.value;
  await updateNode(node);
});

inspector.terminal.addEventListener('change', async () => {
  const node = state.nodes.find(n => n.nodeKey === state.selectedNodeKey);
  if (!node) return;
  node.terminalType = inspector.terminal.value;
  node.terminal = inspector.terminal.value !== 'NONE';
  await updateNode(node);
  render();
});

inspector.choices.addEventListener('click', (event) => {
  const btn = event.target.closest('.connector-btn');
  if (!btn) return;
  const choiceKey = btn.getAttribute('data-choice-id');
  const targetType = btn.getAttribute('data-target');
  state.connectMode = { choiceKey, targetType };
  btn.classList.toggle('active');
});

inspector.choices.addEventListener('input', async (event) => {
  const target = event.target;
  const choiceKey = target.getAttribute('data-choice-id');
  const field = target.getAttribute('data-field');
  if (!choiceKey || !field) return;
  const choice = state.choices.find(c => c.choiceKey === choiceKey);
  if (!choice) return;
  choice[field] = target.value;
  await updateChoice(choice);
});

campaignSelect.addEventListener('change', async () => {
  state.campaignKey = campaignSelect.value;
  await loadGraph();
});

document.getElementById('addNodeBtn').onclick = () => createNode();
document.getElementById('addChoiceBtn').onclick = () => createChoice();
document.getElementById('autoLayoutBtn').onclick = () => applyAutoLayout();

document.getElementById('exportBtn').onclick = () => {
  navigator.clipboard.writeText(JSON.stringify({ nodes: state.nodes, choices: state.choices }, null, 2));
  alert('JSON скопирован в буфер.');
};

(async function init() {
  await loadCampaigns();
  await loadGraph();
  updateViewportTransform();
})();
