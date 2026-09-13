import { useEffect, useRef, useState } from 'react'

interface Option<T extends string | number> {
  value: T
  label: string
}

interface Props<T extends string | number> {
  value: T
  options: Option<T>[]
  onChange: (value: T) => void
  className?: string
}

export function SelectDropdown<T extends string | number>({ value, options, onChange, className }: Props<T>) {
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

  const selectedLabel = options.find(o => o.value === value)?.label ?? ''

  return (
    <div className={`dropdown filter-dropdown dropdown-fit${className ? ` ${className}` : ''}`} ref={ref}>
      <button
        type="button"
        className="btn btn-outline-secondary dropdown-toggle filter-dropdown-toggle"
        aria-expanded={open}
        onClick={() => setOpen(o => !o)}
      >
        {selectedLabel}
      </button>

      <ul className={`dropdown-menu${open ? ' show' : ''}`} data-bs-popper="static">
        {options.map(o => (
          <li key={o.value}>
            <button
              type="button"
              className={`dropdown-item${value === o.value ? ' active' : ''}`}
              onClick={() => {
                onChange(o.value)
                setOpen(false)
              }}
            >
              {o.label}
            </button>
          </li>
        ))}
      </ul>
    </div>
  )
}
