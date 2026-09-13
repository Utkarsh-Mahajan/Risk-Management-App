import { useEffect, useRef, useState } from 'react'

interface Option<T extends string> {
  value: T
  label: string
}

interface Props<T extends string> {
  label: string
  value: T | ''
  placeholder: string
  options: Option<T>[]
  onChange: (value: T | '') => void
}

export function FilterDropdown<T extends string>({ label, value, placeholder, options, onChange }: Props<T>) {
  const [open, setOpen] = useState(false)
  const ref = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!open) return

    function onOutsideClick(e: MouseEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false)
    }
    function onEscape(e: KeyboardEvent) {
      if (e.key === 'Escape') setOpen(false)
    }

    document.addEventListener('mousedown', onOutsideClick)
    document.addEventListener('keydown', onEscape)
    return () => {
      document.removeEventListener('mousedown', onOutsideClick)
      document.removeEventListener('keydown', onEscape)
    }
  }, [open])

  function select(next: T | '') {
    onChange(next)
    setOpen(false)
  }

  const selectedLabel = options.find(o => o.value === value)?.label ?? placeholder

  return (
    <div className="dropdown filter-dropdown" ref={ref}>
      <button
        type="button"
        className="btn btn-outline-secondary dropdown-toggle filter-dropdown-toggle"
        aria-expanded={open}
        onClick={() => setOpen(o => !o)}
      >
        <span className="filter-dropdown-label">{label}</span>
        {selectedLabel}
      </button>

      {/* data-bs-popper="static" reuses Bootstrap's own CSS rule for below-placement without loading its JS/Popper */}
      <ul className={`dropdown-menu${open ? ' show' : ''}`} data-bs-popper="static">
        <li>
          <button type="button" className={`dropdown-item${value === '' ? ' active' : ''}`} onClick={() => select('')}>
            {placeholder}
          </button>
        </li>
        {options.map(o => (
          <li key={o.value}>
            <button
              type="button"
              className={`dropdown-item${value === o.value ? ' active' : ''}`}
              onClick={() => select(o.value)}
            >
              {o.label}
            </button>
          </li>
        ))}
      </ul>
    </div>
  )
}
