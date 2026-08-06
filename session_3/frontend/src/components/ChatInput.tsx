import { useState, type FormEvent } from 'react'

interface ChatInputProps {
  onSend: (message: string) => void
  disabled?: boolean
}

const SUGGESTIONS = [
  'What company data is available in the database?',
  'Help me with a finance-related question.',
  'Search the knowledge base for internal policies.',
]

export function ChatInput({ onSend, disabled }: ChatInputProps) {
  const [input, setInput] = useState('')

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault()
    if (!input.trim()) return
    onSend(input)
    setInput('')
  }

  return (
    <div className="chat-input-area">
      <div className="suggestions">
        {SUGGESTIONS.map((suggestion) => (
          <button
            key={suggestion}
            type="button"
            className="suggestion-chip"
            disabled={disabled}
            onClick={() => onSend(suggestion)}
          >
            {suggestion}
          </button>
        ))}
      </div>

      <form className="chat-form" onSubmit={handleSubmit}>
        <textarea
          value={input}
          onChange={(event) => setInput(event.target.value)}
          onKeyDown={(event) => {
            if (event.key === 'Enter' && !event.shiftKey) {
              event.preventDefault()
              handleSubmit(event)
            }
          }}
          placeholder="Ask about finance, IT, marketing, sales, or company documents..."
          rows={2}
          disabled={disabled}
          aria-label="Chat message"
        />
        <button type="submit" disabled={disabled || !input.trim()}>
          {disabled ? 'Thinking...' : 'Send'}
        </button>
      </form>
    </div>
  )
}
