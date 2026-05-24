(function(){
  const WIDTH_PREFIX = 'erp.tableWidths.';

  function escapeHtml(s){
    return String(s == null ? '' : s).replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  }

  function rows(table){
    return Array.from(table.tBodies[0]?.querySelectorAll('tr') || []).filter(tr => !tr.querySelector('td[colspan]'));
  }

  function tableKey(table){
    return table.dataset.tableKey || table.id || location.pathname.replace(/[^\w-]+/g, '_');
  }

  function measureColumnWidth(table, idx){
    const th = table.querySelectorAll('thead th')[idx];
    const sample = [th].concat(rows(table).map(tr => tr.children[idx]).filter(Boolean));
    const measurer = document.createElement('span');
    measurer.className = 'column-width-measurer';
    measurer.style.position = 'absolute';
    measurer.style.visibility = 'hidden';
    measurer.style.whiteSpace = 'nowrap';
    measurer.style.left = '-99999px';
    document.body.appendChild(measurer);

    let max = 70;
    sample.forEach(cell => {
      const style = window.getComputedStyle(cell);
      measurer.style.font = style.font;
      // Clone header text without tool buttons/resizer noise.
      let text = '';
      if (cell.tagName && cell.tagName.toLowerCase() === 'th') {
        text = Array.from(cell.childNodes).filter(n => n.nodeType === Node.TEXT_NODE).map(n => n.textContent).join('').trim() || cell.innerText.trim();
      } else {
        text = cell.innerText.trim();
      }
      measurer.textContent = text || '';
      max = Math.max(max, Math.ceil(measurer.getBoundingClientRect().width) + 36);
    });
    measurer.remove();
    return Math.min(Math.max(max, 80), 420);
  }

  function setColumnWidth(table, idx, width){
    const th = table.querySelectorAll('thead th')[idx];
    if (!th) return;
    const w = Math.max(70, Math.round(width));
    const col = ensureColGroup(table).children[idx];
    if (col) col.style.setProperty('width', w + 'px', 'important');
    th.style.setProperty('width', w + 'px', 'important');
    th.style.setProperty('min-width', w + 'px', 'important');
    th.style.setProperty('max-width', w + 'px', 'important');
    applyTableMinWidth(table);
  }

  function autoFitColumn(table, idx){
    setColumnWidth(table, idx, measureColumnWidth(table, idx));
    saveWidths(table);
  }

  function autoFitAllColumns(table){
    Array.from(table.querySelectorAll('thead th')).forEach((th, idx) => setColumnWidth(table, idx, measureColumnWidth(table, idx)));
    saveWidths(table);
  }

  function ensureColGroup(table){
    let colgroup = table.querySelector('colgroup[data-generated="true"]');
    const count = table.querySelectorAll('thead th').length;
    if (!colgroup) {
      colgroup = document.createElement('colgroup');
      colgroup.dataset.generated = 'true';
      for (let i = 0; i < count; i++) colgroup.appendChild(document.createElement('col'));
      table.insertBefore(colgroup, table.firstChild);
    } else {
      while (colgroup.children.length < count) colgroup.appendChild(document.createElement('col'));
      while (colgroup.children.length > count) colgroup.removeChild(colgroup.lastChild);
    }
    return colgroup;
  }

  function saveWidths(table){
    const key = WIDTH_PREFIX + tableKey(table);
    const cols = Array.from(ensureColGroup(table).children);
    const ths = Array.from(table.querySelectorAll('thead th'));
    const widths = ths.map((th, idx) => {
      const colWidth = parseFloat(cols[idx]?.style.width || '0');
      const thWidth = parseFloat(th.style.width || '0');
      return Math.round(colWidth || thWidth || 0);
    });
    try {
      localStorage.setItem(key, JSON.stringify(widths));
    } catch(e) {
      console.warn('無法儲存欄寬設定', e);
    }
  }

  function loadWidths(table){
    try {
      const widths = JSON.parse(localStorage.getItem(WIDTH_PREFIX + tableKey(table)) || 'null');
      return Array.isArray(widths) ? widths.map(v => Number(v) || 0) : null;
    } catch(e) { return null; }
  }

  function applyTableMinWidth(table){
    const cols = Array.from(ensureColGroup(table).children);
    const ths = Array.from(table.querySelectorAll('thead th'));
    const sum = ths.reduce((acc, th, idx) => {
      if (th.style.display === 'none') return acc;
      const colW = parseFloat(cols[idx]?.style.width || '0');
      const thW = parseFloat(th.style.width || '0');
      const realW = Math.round(th.getBoundingClientRect().width || 0);
      return acc + Math.max(70, colW || thW || realW || 0);
    }, 0);
    if (sum > 0) {
      table.style.setProperty('min-width', sum + 'px', 'important');
      table.style.setProperty('width', sum + 'px', 'important');
    }
  }

  function initResizableTable(table){
    if (table.dataset.resizableReady === '1') return;
    table.dataset.resizableReady = '1';
    table.style.tableLayout = 'fixed';

    const ths = Array.from(table.querySelectorAll('thead th'));
    const saved = loadWidths(table);

    ths.forEach((th, idx) => {
      th.classList.add('resizable-th');
      const initial = saved && saved[idx] ? saved[idx] : measureColumnWidth(table, idx);
      setColumnWidth(table, idx, initial);

      const grip = document.createElement('span');
      grip.className = 'column-resizer no-print';
      grip.title = '拖曳調整欄寬；點一下自動符合最寬資料';
      th.appendChild(grip);

      let startX = 0, startW = 0, moved = false;
      grip.addEventListener('mousedown', e => {
        e.preventDefault();
        e.stopPropagation();
        startX = e.clientX;
        startW = th.getBoundingClientRect().width;
        moved = false;
        document.body.classList.add('resizing-column');

        function move(ev){
          const delta = ev.clientX - startX;
          if (Math.abs(delta) > 3) moved = true;
          const w = Math.max(70, startW + delta);
          setColumnWidth(table, idx, w);
        }

        function up(){
          document.removeEventListener('mousemove', move);
          document.removeEventListener('mouseup', up);
          document.body.classList.remove('resizing-column');
          if (!moved) autoFitColumn(table, idx);
          else saveWidths(table);
        }

        document.addEventListener('mousemove', move);
        document.addEventListener('mouseup', up);
      });

      grip.addEventListener('dblclick', e => {
        e.preventDefault();
        e.stopPropagation();
        autoFitColumn(table, idx);
      });
    });

    applyTableMinWidth(table);
  }

  function uniqueValues(table, colIndex){
    return Array.from(new Set(rows(table).map(tr => (tr.children[colIndex]?.innerText || '').trim()).filter(Boolean))).sort((a,b) => a.localeCompare(b, 'zh-Hant'));
  }

  function applyFilters(table){
    const filters = table._activeFilters || new Map();
    rows(table).forEach(tr => {
      let visible = true;
      filters.forEach((selected, colIndex) => {
        const value = (tr.children[colIndex]?.innerText || '').trim();
        if (selected.size === 0 || !selected.has(value)) visible = false;
      });
      tr.style.display = visible ? '' : 'none';
    });

    table.querySelectorAll('th[data-filterable="true"]').forEach((th, idx) => {
      const clear = th.querySelector('.filter-clear');
      if (clear) clear.style.display = filters.has(idx) ? 'inline-flex' : 'none';
    });
  }

  function closeFilterMenus(){
    document.querySelectorAll('.excel-filter-menu, .column-chooser-menu').forEach(m => m.remove());
  }

  function openFilterMenu(table, th, colIndex, button){
    closeFilterMenus();

    const values = uniqueValues(table, colIndex);
    const current = table._activeFilters?.get(colIndex);
    const selected = new Set(current ? Array.from(current) : values);

    const menu = document.createElement('div');
    menu.className = 'excel-filter-menu no-print';
    menu.innerHTML = `
      <div class="excel-filter-menu-title">${escapeHtml(th.dataset.filterLabel || th.childNodes[0]?.textContent?.trim() || '篩選')}</div>
      <input class="excel-filter-search" placeholder="搜尋">
      <label class="excel-filter-item excel-filter-all">
        <input type="checkbox" data-all="1"> <span>(全選)</span>
      </label>
      <div class="excel-filter-options"></div>
      <div class="excel-filter-actions">
        <button type="button" class="btn btn-primary" data-apply="1">套用</button>
        <button type="button" class="btn btn-outline" data-clear="1">清除篩選</button>
      </div>
    `;
    document.body.appendChild(menu);

    const rect = button.getBoundingClientRect();
    menu.style.left = Math.min(rect.left, window.innerWidth - 340) + 'px';
    menu.style.top = Math.min(rect.bottom + 8, window.innerHeight - 530) + 'px';

    const search = menu.querySelector('.excel-filter-search');
    const all = menu.querySelector('input[data-all]');
    const options = menu.querySelector('.excel-filter-options');

    function render(){
      const key = search.value.trim().toLowerCase();
      const filtered = values.filter(v => v.toLowerCase().includes(key));
      options.innerHTML = filtered.map(v => `
        <label class="excel-filter-item">
          <input type="checkbox" value="${escapeHtml(v)}" ${selected.has(v) ? 'checked' : ''}> <span>${escapeHtml(v)}</span>
        </label>
      `).join('');

      const inputs = Array.from(options.querySelectorAll('input[type="checkbox"]'));
      const checkedCount = inputs.filter(i => i.checked).length;
      all.checked = inputs.length > 0 && checkedCount === inputs.length;
      all.indeterminate = checkedCount > 0 && checkedCount < inputs.length;

      inputs.forEach(input => {
        input.addEventListener('change', () => {
          if (input.checked) selected.add(input.value);
          else selected.delete(input.value);
          const visibleInputs = Array.from(options.querySelectorAll('input[type="checkbox"]'));
          const c = visibleInputs.filter(i => i.checked).length;
          all.checked = visibleInputs.length > 0 && c === visibleInputs.length;
          all.indeterminate = c > 0 && c < visibleInputs.length;
        });
      });
    }

    search.addEventListener('input', render);
    all.addEventListener('change', () => {
      const inputs = Array.from(options.querySelectorAll('input[type="checkbox"]'));
      if (all.checked) inputs.forEach(i => { i.checked = true; selected.add(i.value); });
      else inputs.forEach(i => { i.checked = false; selected.delete(i.value); });
      all.indeterminate = false;
    });

    menu.querySelector('[data-apply]').addEventListener('click', () => {
      if (!table._activeFilters) table._activeFilters = new Map();
      if (selected.size === values.length) table._activeFilters.delete(colIndex);
      else table._activeFilters.set(colIndex, new Set(selected));
      applyFilters(table);
      closeFilterMenus();
    });

    menu.querySelector('[data-clear]').addEventListener('click', () => {
      if (!table._activeFilters) table._activeFilters = new Map();
      table._activeFilters.delete(colIndex);
      applyFilters(table);
      closeFilterMenus();
    });

    render();
    setTimeout(() => search.focus(), 20);
  }

  function initFiltersAndSort(table){
    if (table.dataset.filterReady === '1') return;
    table.dataset.filterReady = '1';
    table._activeFilters = table._activeFilters || new Map();

    Array.from(table.querySelectorAll('thead th')).forEach((th, idx) => {
      if (th.dataset.filterable === 'true') {
        th.classList.add('filterable-th');
        const trigger = document.createElement('button');
        trigger.type = 'button';
        trigger.className = 'excel-filter-trigger no-print';
        trigger.textContent = '▾';
        trigger.title = '篩選';
        trigger.addEventListener('click', e => {
          e.preventDefault();
          e.stopPropagation();
          openFilterMenu(table, th, idx, trigger);
        });

        const clear = document.createElement('button');
        clear.type = 'button';
        clear.className = 'filter-clear no-print';
        clear.textContent = '×';
        clear.title = '清除本欄篩選';
        clear.style.display = 'none';
        clear.addEventListener('click', e => {
          e.preventDefault();
          e.stopPropagation();
          table._activeFilters.delete(idx);
          applyFilters(table);
        });

        th.appendChild(clear);
        th.appendChild(trigger);
      }

      if (th.dataset.sort) {
        th.classList.add('sortable-th');
        th.addEventListener('click', e => {
          if (e.target.closest('button') || e.target.closest('.column-resizer')) return;
          const type = th.dataset.sort || 'text';
          const tbody = table.tBodies[0];
          const dir = th.dataset.dir === 'asc' ? 'desc' : 'asc';
          th.dataset.dir = dir;
          const sorted = rows(table).sort((a,b) => {
            let av = a.children[idx] ? a.children[idx].innerText.trim() : '';
            let bv = b.children[idx] ? b.children[idx].innerText.trim() : '';
            if (type === 'number') {
              av = Number(av.replace(/,/g,'')) || 0;
              bv = Number(bv.replace(/,/g,'')) || 0;
              return dir === 'asc' ? av - bv : bv - av;
            }
            return dir === 'asc' ? av.localeCompare(bv, 'zh-Hant') : bv.localeCompare(av, 'zh-Hant');
          });
          sorted.forEach(tr => tbody.appendChild(tr));
          table.querySelectorAll('th[data-sort]').forEach(h => { if (h !== th) h.removeAttribute('data-dir'); });
          applyFilters(table);
        });
      }
    });

    applyFilters(table);
  }

  function init(){
    document.querySelectorAll('table.resizable-table').forEach(table => {
      initResizableTable(table);
      applyVisibleColumns(table);
      applyTableMinWidth(table);
    });
    document.querySelectorAll('table.filterable-table').forEach(initFiltersAndSort);
    document.addEventListener('click', e => {
      if (!e.target.closest('.excel-filter-menu') && !e.target.closest('.column-chooser-menu') && !e.target.closest('.excel-filter-trigger') && !e.target.closest('[onclick^="openColumnChooser"]')) closeFilterMenus();
    });
  }


  const VISIBLE_PREFIX = 'erp.visibleColumns.';

  function loadVisibleColumns(table){
    try {
      const raw = localStorage.getItem(VISIBLE_PREFIX + tableKey(table));
      if (!raw) return null;
      const arr = JSON.parse(raw);
      return Array.isArray(arr) ? arr.map(Boolean) : null;
    } catch(e) { return null; }
  }

  function saveVisibleColumns(table, visible){
    localStorage.setItem(VISIBLE_PREFIX + tableKey(table), JSON.stringify(visible));
  }

  function setColumnVisible(table, idx, visible){
    const display = visible ? '' : 'none';
    const th = table.querySelectorAll('thead th')[idx];
    if (th) th.style.display = display;
    rows(table).forEach(tr => {
      if (tr.children[idx]) tr.children[idx].style.display = display;
    });
    // Keep empty-row colspan roughly aligned.
    Array.from(table.tBodies[0]?.querySelectorAll('td[colspan]') || []).forEach(td => {
      const total = table.querySelectorAll('thead th').length;
      const hidden = Array.from(table.querySelectorAll('thead th')).filter(h => h.style.display === 'none').length;
      td.colSpan = Math.max(1, total - hidden);
    });
    applyTableMinWidth(table);
  }

  function applyVisibleColumns(table){
    const ths = Array.from(table.querySelectorAll('thead th'));
    const saved = loadVisibleColumns(table);
    const visible = saved && saved.length === ths.length ? saved : ths.map(() => true);
    visible.forEach((v, idx) => setColumnVisible(table, idx, v));
  }

  function openColumnChooser(tableId){
    const table = document.getElementById(tableId);
    if (!table) {
      alert('找不到表格。');
      return;
    }
    document.querySelectorAll('.column-chooser-menu').forEach(m => m.remove());

    const ths = Array.from(table.querySelectorAll('thead th'));
    const visible = ths.map(th => th.style.display !== 'none');
    const menu = document.createElement('div');
    menu.className = 'column-chooser-menu no-print';
    menu.innerHTML = `
      <div class="excel-filter-menu-title">顯示欄位</div>
      <label class="excel-filter-item excel-filter-all">
        <input type="checkbox" data-all="1"> <span>(全選)</span>
      </label>
      <div class="excel-filter-options">
        ${ths.map((th, idx) => {
          const label = (th.dataset.filterLabel || Array.from(th.childNodes).filter(n => n.nodeType === Node.TEXT_NODE).map(n => n.textContent).join('').trim() || th.innerText.trim() || ('欄位' + (idx + 1))).replace(/[▾×↑↓]/g, '').trim();
          return `<label class="excel-filter-item"><input type="checkbox" data-col="${idx}" ${visible[idx] ? 'checked' : ''}> <span>${escapeHtml(label)}</span></label>`;
        }).join('')}
      </div>
      <div class="excel-filter-actions">
        <button type="button" class="btn btn-primary" data-apply="1">套用</button>
        <button type="button" class="btn btn-outline" data-reset="1">全部顯示</button>
      </div>
    `;
    document.body.appendChild(menu);

    const active = document.activeElement;
    const rect = active && active.getBoundingClientRect ? active.getBoundingClientRect() : {left: 40, bottom: 80};
    menu.style.left = Math.min(rect.left, window.innerWidth - 340) + 'px';
    menu.style.top = Math.min(rect.bottom + 8, window.innerHeight - 530) + 'px';

    const all = menu.querySelector('input[data-all]');
    function refreshAll(){
      const inputs = Array.from(menu.querySelectorAll('input[data-col]'));
      const checked = inputs.filter(i => i.checked).length;
      all.checked = inputs.length > 0 && checked === inputs.length;
      all.indeterminate = checked > 0 && checked < inputs.length;
    }
    refreshAll();

    all.addEventListener('change', () => {
      menu.querySelectorAll('input[data-col]').forEach(i => i.checked = all.checked);
      all.indeterminate = false;
    });
    menu.querySelectorAll('input[data-col]').forEach(i => i.addEventListener('change', refreshAll));

    menu.querySelector('[data-apply]').addEventListener('click', () => {
      const chosen = ths.map((th, idx) => {
        const input = menu.querySelector(`input[data-col="${idx}"]`);
        return !!(input && input.checked);
      });
      saveVisibleColumns(table, chosen);
      chosen.forEach((v, idx) => setColumnVisible(table, idx, v));
      menu.remove();
    });
    menu.querySelector('[data-reset]').addEventListener('click', () => {
      const chosen = ths.map(() => true);
      saveVisibleColumns(table, chosen);
      chosen.forEach((v, idx) => setColumnVisible(table, idx, v));
      menu.remove();
    });
  }

  window.openColumnChooser = openColumnChooser;

  function csvCell(text){
    const s = String(text == null ? '' : text).replace(/\r?\n|\r/g, ' ').trim();
    return '"' + s.replace(/"/g, '""') + '"';
  }

  function downloadTableCsv(tableId, filename){
    const table = document.getElementById(tableId);
    if (!table) {
      alert('找不到可下載的表格。');
      return;
    }
    const lines = [];
    const trs = Array.from(table.querySelectorAll('tr')).filter(tr => tr.style.display !== 'none');
    trs.forEach(tr => {
      const cells = Array.from(tr.children).filter(cell => !cell.classList.contains('no-print') && cell.style.display !== 'none');
      if (!cells.length) return;
      lines.push(cells.map(cell => {
        const clone = cell.cloneNode(true);
        clone.querySelectorAll('button, .column-resizer, .excel-filter-trigger, .filter-clear, script, style').forEach(n => n.remove());
        return csvCell(clone.innerText);
      }).join(','));
    });
    const blob = new Blob(["\ufeff" + lines.join("\n")], { type: 'text/csv;charset=utf-8;' });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = filename || 'export.csv';
    document.body.appendChild(a);
    a.click();
    URL.revokeObjectURL(a.href);
    a.remove();
  }

  window.downloadTableCsv = downloadTableCsv;

  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', init);
  else init();
})();
