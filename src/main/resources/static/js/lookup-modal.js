
(function(){
  const typeToUrl = {
    customers: '/api/lookup/customers',
    suppliers: '/api/lookup/suppliers',
    products: '/api/lookup/products',
    labels: '/api/lookup/label-templates'
  };

  const selectNameToType = {
    customerId: 'customers',
    supplierId: 'suppliers',
    productId: 'products',
    newProductId: 'products',
    labelSettingId: 'labels',
    newLabelSettingId: 'labels'
  };

  function applyFixedModalStyles(modal){
    Object.assign(modal.style, {
      position: 'fixed',
      inset: '0',
      zIndex: '99999',
      background: 'rgba(15,23,42,.38)',
      display: 'none',
      alignItems: 'center',
      justifyContent: 'center',
      padding: '24px',
      boxSizing: 'border-box'
    });
  }

  function ensureModal(){
    let modal = document.getElementById('lookupModal');
    if (modal) {
      applyFixedModalStyles(modal);
      return modal;
    }
    modal = document.createElement('div');
    modal.id = 'lookupModal';
    modal.className = 'lookup-modal-backdrop';
    applyFixedModalStyles(modal);
    modal.innerHTML = `
      <div class="lookup-modal" style="width:min(720px,96vw);max-height:82vh;overflow:hidden;background:#fff;border-radius:18px;box-shadow:0 22px 70px rgba(15,23,42,.25);border:1px solid #e5e7eb;">
        <div class="lookup-modal-header" style="padding:16px 18px;border-bottom:1px solid #e5e7eb;display:flex;justify-content:space-between;align-items:center;">
          <strong id="lookupTitle">查詢</strong>
          <button type="button" class="btn btn-outline" id="lookupClose">關閉</button>
        </div>
        <div class="lookup-modal-body" style="padding:18px;">
          <input id="lookupSearch" placeholder="輸入關鍵字後查詢">
          <div class="lookup-actions" style="margin-top:12px;"><button type="button" class="btn btn-primary" id="lookupSearchBtn">查詢</button></div>
          <div id="lookupResults" class="lookup-results" style="margin-top:14px;max-height:48vh;overflow:auto;border:1px solid #e5e7eb;border-radius:12px;"></div>
        </div>
      </div>`;
    document.body.appendChild(modal);
    document.getElementById('lookupClose').addEventListener('click', () => modal.style.display = 'none');
    modal.addEventListener('click', e => { if (e.target === modal) modal.style.display = 'none'; });
    return modal;
  }

  function titleFor(type){
    if (type === 'customers') return '查詢客戶';
    if (type === 'suppliers') return '查詢供應商';
    if (type === 'products') return '查詢品項';
    if (type === 'labels') return '查詢貼紙範本';
    return '查詢';
  }

  async function fetchRows(type, q){
    const url = typeToUrl[type];
    if (!url) return [];
    const res = await fetch(url + '?q=' + encodeURIComponent(q || ''));
    if (!res.ok) return [];
    return await res.json();
  }

  async function fetchById(type, id){
    const url = typeToUrl[type];
    if (!url || !id) return null;
    try {
      const res = await fetch(url + '?id=' + encodeURIComponent(id));
      const rows = await res.json();
      return rows && rows.length ? rows[0] : null;
    } catch(e) { return null; }
  }

  function setError(input, message){
    clearError(input);
    input.classList.add('field-error');
    const msg = document.createElement('div');
    msg.className = 'field-error-text';
    msg.textContent = message || '必填';
    input.insertAdjacentElement('afterend', msg);
  }

  function clearError(input){
    input.classList.remove('field-error');
    const next = input.nextElementSibling;
    if (next && next.classList.contains('field-error-text')) next.remove();
  }

  function fillInputAndPairs(input, row){
    const fill = input.dataset.fill || 'label';
    if (fill === 'code') input.value = row.code || row.label || '';
    else if (fill === 'name') input.value = row.name || row.label || '';
    else input.value = row.label || '';

    if (input.dataset.pairCode) {
      const codeInput = document.getElementById(input.dataset.pairCode);
      if (codeInput) { codeInput.value = row.code || ''; clearError(codeInput); }
    }
    if (input.dataset.pairName) {
      const nameInput = document.getElementById(input.dataset.pairName);
      if (nameInput) { nameInput.value = row.name || ''; clearError(nameInput); }
    }
    clearError(input);
  }

  async function openLookup(input, options){
    const type = input.dataset.lookup;
    const hidden = options && options.hidden ? options.hidden : (input.dataset.targetId ? document.getElementById(input.dataset.targetId) : null);
    if (!type) return;
    const modal = ensureModal();
    const search = document.getElementById('lookupSearch');
    const results = document.getElementById('lookupResults');
    const btn = document.getElementById('lookupSearchBtn');
    document.getElementById('lookupTitle').innerText = titleFor(type);
    search.value = input.value || '';
    results.innerHTML = '';
    modal.style.display = 'flex';

    async function run(){
      const rows = await fetchRows(type, search.value);
      if (!rows.length) {
        results.innerHTML = '<div class="lookup-empty" style="padding:14px;color:#6b7280;">查無資料</div>';
        return;
      }
      results.innerHTML = rows.map(r => `
        <button type="button" class="lookup-row" style="width:100%;border:0;border-bottom:1px solid #e5e7eb;border-radius:0;background:#fff;text-align:left;padding:12px 14px;color:#111827;display:block;" data-id="${r.id}" data-label="${escapeHtml(r.label || '')}" data-code="${escapeHtml(r.code || '')}" data-name="${escapeHtml(r.name || '')}">
          <span>${escapeHtml(r.label || '')}</span>
        </button>`).join('');
      results.querySelectorAll('.lookup-row').forEach(row => {
        row.addEventListener('mouseenter', () => row.style.background = '#eff6ff');
        row.addEventListener('mouseleave', () => row.style.background = '#fff');
        row.addEventListener('click', () => {
          const data = {id: row.dataset.id, label: row.dataset.label, code: row.dataset.code, name: row.dataset.name};
          if (hidden) hidden.value = data.id;
          fillInputAndPairs(input, data);
          input.dispatchEvent(new Event('change', {bubbles:true}));
          modal.style.display = 'none';
        });
      });
    }

    btn.onclick = run;
    search.onkeydown = e => { if (e.key === 'Enter') { e.preventDefault(); run(); } };
    await run();
    setTimeout(() => { search.focus(); search.select(); }, 30);
  }

  function enhanceSelect(select){
    if (select.dataset.lookupEnhanced === '1') return;
    const type = select.dataset.lookupType || selectNameToType[select.name];
    if (!type) return;
    select.dataset.lookupEnhanced = '1';

    const isRequired = select.required || select.dataset.required === 'true';

    const hidden = document.createElement('input');
    hidden.type = 'hidden';
    hidden.name = select.name;
    hidden.value = select.value || '';
    if (isRequired) hidden.dataset.requiredLookup = 'true';

    const input = document.createElement('input');
    input.type = 'text';
    input.dataset.lookup = type;
    input.dataset.lookupInput = 'true';
    input.placeholder = isRequired ? '必填：輸入關鍵字，雙擊查詢選擇' : '可輸入關鍵字，雙擊查詢選擇';
    input.value = select.selectedOptions && select.selectedOptions.length ? select.selectedOptions[0].textContent.trim() : '';
    if (!select.value) input.value = '';
    if (isRequired) input.dataset.requiredLookupInput = 'true';

    select.removeAttribute('name');
    select.required = false;
    select.style.display = 'none';
    select.parentNode.insertBefore(hidden, select);
    select.parentNode.insertBefore(input, select);

    input.addEventListener('dblclick', () => openLookup(input, {hidden}));
    input.addEventListener('input', () => {
      hidden.value = '';
      if (input.value.trim() !== '') clearError(input);
    });
  }

  function validateForm(form){
    let ok = true;
    form.querySelectorAll('input[data-required-lookup="true"]').forEach(hidden => {
      if (!hidden.value) {
        ok = false;
        let input = null;
        if (hidden.dataset.errorTarget) input = document.getElementById(hidden.dataset.errorTarget);
        if (!input) input = hidden.nextElementSibling;
        if (input) setError(input, '必填，請雙擊查詢並選擇正確資料');
      }
    });
    return ok;
  }

  function escapeHtml(s){
    return String(s).replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  }

  document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('select[name="customerId"],select[name="supplierId"],select[name="productId"],select[name="newProductId"],select[name="labelSettingId"],select[name="newLabelSettingId"]').forEach(enhanceSelect);

    document.querySelectorAll('input[data-lookup]').forEach(input => {
      if (input.dataset.lookupInput === 'true') return;
      input.title = '可輸入關鍵字後雙擊查詢，或直接雙擊列出資料';
      input.addEventListener('dblclick', () => openLookup(input));
      input.addEventListener('input', () => {
        const hidden = input.dataset.targetId ? document.getElementById(input.dataset.targetId) : null;
        if (hidden) hidden.value = '';
      });
      const hidden = input.dataset.targetId ? document.getElementById(input.dataset.targetId) : null;
      if (hidden && hidden.value && !input.value) {
        fetchById(input.dataset.lookup, hidden.value).then(row => { if (row) fillInputAndPairs(input, row); });
      }
    });

    document.querySelectorAll('form').forEach(form => {
      form.addEventListener('submit', e => {
        if (!validateForm(form)) {
          e.preventDefault();
          e.stopPropagation();
        }
      });
    });
  });
})();
